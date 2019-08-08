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

import scala.concurrent.{ ExecutionContext, Future }

import akka.util.ByteString
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject._
import org.joda.time.DateTime
import play.api._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.http.HttpEntity
import play.api.mvc._
import slick.jdbc.JdbcProfile

import auth.DefaultEnv
import daos.PictureDAO
import models.{ Picture }
import pictures.{
  BoundPicture, CoverPicture, MaxPicture, PictureCache, PictureTransform,
  PictureUtils, RawPicture }

@Singleton
class PictureController @Inject() (
  cc: ControllerComponents,
  implicit val config: Configuration,
  protected val dbConfigProvider: DatabaseConfigProvider,
  pictureDao: PictureDAO,
  pictureCache: PictureCache,
  silhouette: Silhouette[DefaultEnv])
  (implicit executionContext: ExecutionContext)
  extends AbstractController(cc)
  with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  /** Receives a new picture and stores it in the database. */
  def upload = silhouette.SecuredAction(parse.multipartFormData).async {
    implicit request =>
      request.body
        .file("picture")
        .map { file =>
          PictureUtils.fromFile(file.ref.path) match {
            case Some(newPic) => {
              pictureDao.insertIfNotExits(newPic).
                map { pic =>
                  val id = pic.base64Id
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

    val idBytes = java.util.Base64.getDecoder.decode(id)

    pictureCache.get(idBytes, transform).
      map {
        case Some(pic) => {
          val bs = ByteString(pic.content)
          Result(
            header = ResponseHeader(200, Map(
              // Cacheable, expires after a year.
              "Cache-Control" -> "public, max-age=31536000")),
            body = HttpEntity.Strict(bs, None)
          )
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