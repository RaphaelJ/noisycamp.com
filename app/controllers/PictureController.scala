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

import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

import akka.util.ByteString
import com.sksamuel.scrimage.Format
import org.joda.time.DateTime
import play.api._
import play.api.http.HttpEntity
import play.api.mvc._

import models.{ Picture, PictureId }
import pictures.{
  BoundPicture, CoverPicture, MaxPicture, PictureCache, PictureTransform,
  PictureUtils, RawPicture }

@Singleton
class PictureController @Inject() (
  ccc: CustomControllerCompoments,
  pictureCache: PictureCache)
  extends CustomBaseController(ccc) {

  import profile.api._

  /** Receives a new picture and stores it in the database. */
  def upload = SecuredAction(parse.multipartFormData).async {
    implicit request =>

    request.body
      .file("picture")
      .map { file =>
        PictureUtils.fromFile(file.ref.path) match {
          case Some(newPic) => {
            daos.picture.insertIfNotExits(newPic).
              map { pic =>
                val id = pic.id.base64
                val uri = routes.PictureController.view(id).url
                Created(id).withHeaders("Location" -> uri)
              }
          }
          case None => Future.successful(BadRequest("Invalid image format."))
        }
      }.
      getOrElse {
        Future.successful(BadRequest("Missing file."))
      }
  }

  def view(id: String) = Action.async { picWithTransform(id, RawPicture) }

  def bound(id: String, size: String) =
    Action.async { picWithSizeTransform(id, size, BoundPicture) }

  def cover(id: String, size: String) =
    Action.async { picWithSizeTransform(id, size, CoverPicture) }

  def max(id: String, size: String) =
    Action.async { picWithSizeTransform(id, size, MaxPicture) }

  // --

  /** Parses image size URL parameter such as "640x480". */
  private def parseSizeArg(sizeArg: String): Option[(Int, Int)] = {
    val parser = raw"(\d+)x(\d+)".r
    sizeArg match {
      case parser(width, height) => {
        Some((Integer.parseInt(width), Integer.parseInt(height)))
      }
      case _ => None
    }
  }

  /** Serves the provided picture with the specified transform. */
  private def picWithTransform(id: String,
    transform: PictureTransform): Future[Result] = {

    val picId = PictureId.fromString(id)

    pictureCache.get(picId, transform).
      map {
        case Some(pic) => {
          val bs = ByteString(pic.content)
          val contentType = pic.format match {
            case Format.PNG => "image/png"
            case Format.GIF => "image/gif"
            case Format.JPEG => "image/jpeg"
          }

          Result(
            header = ResponseHeader(200, Map(
              // Cacheable, expires after a year.
              "Cache-Control" -> "public, max-age=31536000")),
            body = HttpEntity.Strict(bs, Some(contentType)))
        }
        case None => NotFound("Picture not found.")
      }
  }

  /** Serves the provided picture with the specified size-parametered transform. */
  private def picWithSizeTransform(id: String, size: String,
    transform: (Int, Int) => PictureTransform) = {

    parseSizeArg(size).
      map { case (width, height) =>
        picWithTransform(id, transform(width, height)) }.
      getOrElse { Future.successful(BadRequest("Invalid size format.")) }
  }
}
