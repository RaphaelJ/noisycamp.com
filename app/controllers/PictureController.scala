/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package controllers

import java.nio.file.{ Path, Paths }
import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

import akka.util.ByteString
import com.sksamuel.scrimage.format.Format
import org.joda.time.DateTime
import play.api._
import play.api.http.HttpEntity
import play.api.mvc._

import models.{ Picture, PictureFromDatabase, PictureFromStream, PictureId, PictureSource }
import pictures.{
    BoundPicture, ChainedTransforms, CoverPicture, LegacyFormat, MaxPicture, OptimizeFormat,
    PictureCache, PictureLoader, PictureTransform }
import views.html.helper.form
import akka.http.scaladsl.settings.PoolImplementation
import akka.actor.Status

@Singleton
class PictureController @Inject() (
    assets: Assets,
    environment: Environment,
    ccc: CustomControllerCompoments,
    pictureCache: PictureCache,
    pictureLoader: PictureLoader)
    extends CustomBaseController(ccc) {

    import profile.api._

    val MAX_UPLOAD_IMAGE_SIZE = 1920

    val ASSET_IMAGE_DIR = Paths.get("public/images")
    val ASSET_IMAGE_ROUTE = Paths.get("/images")

    /** Receives a new picture and stores it in the database. */
    def upload = SecuredAction(parse.multipartFormData).async { implicit request =>

        def savePicture(pic: Picture): Future[Result] = {
            val optimizedPic =
                OptimizeFormat(MaxPicture(MAX_UPLOAD_IMAGE_SIZE, MAX_UPLOAD_IMAGE_SIZE)(pic))

            pictureLoader.
                toDatabase(optimizedPic).
                map { pic =>
                    val id = pic.id.base64
                    val uri = routes.PictureController.view(id).url
                    Created(id).withHeaders("Location" -> uri)
                }
        }

        request.body
            .file("picture")
            .map { file =>
                pictureLoader.
                    fromFile(file.ref.path).
                    flatMap {
                        case Some(pic) => savePicture(pic)
                        case None => Future.successful(BadRequest("Invalid image format."))
                    }
            }.
            getOrElse { Future.successful(BadRequest("Missing file.")) }
    }


    def view(id: String, optimized: Option[Boolean] = None) = Action.async {
        val picId = PictureId.fromString(id)
        picWithTransform(PictureFromDatabase(picId), None, optimized)
    }

    def asset(path: String, optimized: Option[Boolean] = None) = Action.async { implicit request =>
        (parsePathArg(path) match {
            case Some(streamSource) => picWithTransform(streamSource, None, optimized)
            case None => Future.successful(NotFound("Asset not found."))
        }).flatMap { result =>
            val statusCode = result.header.status
            if (statusCode >= 200 && statusCode < 300) {
                Future.successful(result)
            } else {
                // Fallback to Play's assets controller.
                assets.versioned(ASSET_IMAGE_ROUTE.resolve(path).toString).apply(request)
            }
        }
    }

    def bound(id: String, size: String, optimized: Option[Boolean] = None) = Action.async {
        picIdWithSizeTransform(id, size, BoundPicture, optimized)
    }

    def boundAsset(path: String, size: String, optimized: Option[Boolean] = None) = Action.async {
        picAssetWithSizeTransform(path, size, BoundPicture, optimized)
    }

    def cover(id: String, size: String, optimized: Option[Boolean] = None) = Action.async {
        picIdWithSizeTransform(id, size, CoverPicture, optimized)
    }

    def coverAsset(path: String, size: String, optimized: Option[Boolean] = None) = Action.async {
        picAssetWithSizeTransform(path, size, CoverPicture, optimized)
    }

    def max(id: String, size: String, optimized: Option[Boolean] = None) = Action.async {
        picIdWithSizeTransform(id, size, MaxPicture, optimized)
    }

    def maxAsset(path: String, size: String, optimized: Option[Boolean] = None) = Action.async {
        picAssetWithSizeTransform(path, size, MaxPicture, optimized)
    }

    // --

    /** Returns the path to the picture object if it's valid.
     *
     * Returns None if the image does not exist. */
    private def parsePathArg(pathArg: String): Option[PictureFromStream] = {

        val path = Paths.get(pathArg)
        val fullpath = ASSET_IMAGE_DIR.resolve(path).normalize

        if (path.isAbsolute || !fullpath.startsWith(ASSET_IMAGE_DIR)) {
            None
        } else {
            environment.
                resourceAsStream(fullpath.toString).
                map(PictureFromStream(fullpath.toUri, _))
        }
    }

    /** Parses image size URL parameter such as "640x480". */
    private def parseSizeArg(sizeArg: String): Option[(Int, Int)] = {
        val parser = raw"(\d+)x(\d+)".r
        sizeArg match {
            case parser(width, height) => Some((Integer.parseInt(width), Integer.parseInt(height)))
            case _ => None
        }
    }

    /** Serves the provided picture with the specified transform. */
    private def picWithTransform(
        source: PictureSource, transform: Option[PictureTransform],
        optimized: Option[Boolean] = None)
        : Future[Result] = {

        // Combines the optimize transform with the provided transform if required.
        val optimizedTransform = optimized.map {
            case true => OptimizeFormat
            case false => LegacyFormat
        }
        val combinedTransform = (transform, optimizedTransform) match {
            case (Some(t1), Some(t2)) => Some(ChainedTransforms(Seq(t1, t2)))
            case (Some(t1), None) => Some(t1)
            case (None, Some(t2)) => Some(t2)
            case (None, None) => None
        }

        pictureCache.get(source, combinedTransform).
            map {
                case Some(pic) => {
                    val bs = ByteString(pic.bytes)
                    val contentType = pic.format.contentType

                    Result(
                        header = ResponseHeader(200, Map(
                            // Cacheable, expires after a year.
                            "Cache-Control" -> "public, max-age=31536000")),
                        body = HttpEntity.Strict(bs, Some(pic.format.contentType)))
                }
                case None => NotFound("Picture not found.")
            }
    }

    /** Serves the provided picture with the specified size-parametered transform. */
    private def picWithSizeTransform(
        source: PictureSource, size: String, sizeTransform: (Int, Int) => PictureTransform,
        optimized: Option[Boolean] = None,
        ): Future[Result] = {

        parseSizeArg(size).
            map { case (width, height) =>
                picWithTransform(source, Some(sizeTransform(width, height)), optimized)
            }.
            getOrElse { Future.successful(BadRequest("Invalid size format.")) }
    }

    private def picAssetWithSizeTransform(
        pathArg: String, size: String, sizeTransform: (Int, Int) => PictureTransform,
        optimized: Option[Boolean] = None,
        ): Future[Result] = {

        parsePathArg(pathArg) match {
            case Some(streamSource) => {
                picWithSizeTransform(streamSource, size, sizeTransform, optimized)
            }
            case None => Future.successful(NotFound("Asset not found."))
        }
    }

    private def picIdWithSizeTransform(
        id: String, size: String, sizeTransform: (Int, Int) => PictureTransform,
        optimized: Option[Boolean] = None,
        ): Future[Result] = {

        val picId = PictureId.fromString(id)
        picWithSizeTransform(PictureFromDatabase(picId), size, sizeTransform, optimized)
    }
}
