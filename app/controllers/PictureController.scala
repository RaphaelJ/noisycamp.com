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
import daos.{ PictureDAO }
import models.{ Picture, PictureHelper }

@Singleton
class PictureController @Inject() (
  cc: ControllerComponents,
  implicit val config: Configuration,
  protected val dbConfigProvider: DatabaseConfigProvider,
  pictureDao: PictureDAO,
  silhouette: Silhouette[DefaultEnv])
  (implicit executionContext: ExecutionContext)
  extends AbstractController(cc)
  with HasDatabaseConfigProvider[JdbcProfile] {
    
  import profile.api._

  /** Returns the requested picture data. */
  def view(id: String) = Action.async { implicit request =>
    val idBytes = java.util.Base64.getDecoder.decode(id)
    
    val optPic: Future[Option[Picture]] = 
      db.run {
        pictureDao.get(idBytes).
          result.
          headOption
      }
      
    optPic.map {
      case Some(pic) => {
        val bs = ByteString(pic.content)
        Result(
          header = ResponseHeader(200, Map.empty),
          body = HttpEntity.Strict(bs, None)
        )
      }
      case None => NotFound
    }
  }

  /** Receives a new picture and stores it in the database. */
  def upload = silhouette.SecuredAction(parse.multipartFormData).async {
    implicit request =>
      request.body
        .file("picture")
        .map { file =>
          PictureHelper.fromFile(file.ref.path) match {
            case Some(newPic) => {              
              pictureDao.insertIfNotExits(newPic).
                map { pic => 
                  val uri = routes.PictureController.view(pic.base64Id).url
                  Created.withHeaders("Location" -> uri)
                }
            }
            case None => Future.successful(BadRequest("Invalid image format."))
          }
        }.
        getOrElse {
          Future.successful(BadRequest("Missing file."))
        }
  }
}
