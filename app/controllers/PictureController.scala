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
    BoundPicture, CoverPicture, MaxPicture, PictureCache, PictureLoader, PictureTransform,
    RawPicture }
import scala.concurrent.java8.FuturesConvertersImpl
import java.io.InputStream

@Singleton
class PictureController @Inject() (
    environment: Environment,
    ccc: CustomControllerCompoments,
    pictureCache: PictureCache,
    pictureLoader: PictureLoader)
    extends CustomBaseController(ccc) {

    import profile.api._

    val MAX_UPLOAD_IMAGE_SIZE = 1920

    val ASSET_PICTURE_DIR = Paths.get("public/images")

    /** Receives a new picture and stores it in the database. */
    def upload = SecuredAction(parse.multipartFormData).async { implicit request =>

        def savePicture(pic: Picture): Future[Result] = {
            val optimizedPic =
                PictureTransform.optimiseFormat(
                    MaxPicture(MAX_UPLOAD_IMAGE_SIZE, MAX_UPLOAD_IMAGE_SIZE)(pic))

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


    def view(id: String) = Action.async {
        val picId = PictureId.fromString(id)
        picWithTransform(PictureFromDatabase(picId), RawPicture)
    }

    def bound(id: String, size: String) = Action.async {
        picIdWithSizeTransform(id, size, BoundPicture)
    }

    def boundAsset(path: String, size: String) = Action.async {
        picAssetWithSizeTransform(path, size, BoundPicture)
    }

    def cover(id: String, size: String) = Action.async {
        picIdWithSizeTransform(id, size, CoverPicture)
    }

    def coverAsset(path: String, size: String) = Action.async {
        picAssetWithSizeTransform(path, size, CoverPicture)
    }

    def max(id: String, size: String) = Action.async {
        picIdWithSizeTransform(id, size, MaxPicture)
    }

    def maxAsset(path: String, size: String) = Action.async {
        picAssetWithSizeTransform(path, size, MaxPicture)
    }

    // --

    /** Returns the path to the picture object if it's valid. */
    private def parsePathArg(pathArg: String): Option[PictureFromStream] = {
        val path = Paths.get(pathArg)
        val fullpath = ASSET_PICTURE_DIR.resolve(path).normalize

        if (path.isAbsolute || !fullpath.startsWith(ASSET_PICTURE_DIR)) {
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
    private def picWithTransform(source: PictureSource, transform: PictureTransform)
        : Future[Result] = {

        pictureCache.get(source, transform).
            map {
                case Some(pic) => {
                    val bs = ByteString(pic.content)
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
        source: PictureSource, size: String, transform: (Int, Int) => PictureTransform
        ): Future[Result] = {

        parseSizeArg(size).
            map { case (width, height) =>
                picWithTransform(source, transform(width, height))
            }.
            getOrElse { Future.successful(BadRequest("Invalid size format.")) }
    }

    private def picAssetWithSizeTransform(
        pathArg: String, size: String, transform: (Int, Int) => PictureTransform
        ): Future[Result] = {

        parsePathArg(pathArg) match {
            case Some(streamSource) => picWithSizeTransform(streamSource, size, transform)
            case None => Future.successful(Forbidden)
        }
    }

    private def picIdWithSizeTransform(
        id: String, size: String, transform: (Int, Int) => PictureTransform
        ): Future[Result] = {

        val picId = PictureId.fromString(id)
        picWithSizeTransform(PictureFromDatabase(picId), size, transform)
    }
}
