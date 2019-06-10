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

import java.nio.file.Files
import java.security.MessageDigest

import com.mohiva.play.silhouette.api.Silhouette
import com.sksamuel.scrimage.FormatDetector
import javax.inject._
import org.joda.time.DateTime
import play.api._
import play.api.mvc._

import auth.DefaultEnv
import daos.{ PictureDAO }

@Singleton
class PictureController @Inject() (
  cc: ControllerComponents,
  implicit val config: Configuration,
  pictureDao: PictureDAO,
  silhouette: Silhouette[DefaultEnv])
  extends AbstractController(cc) {

  def show(id: String) = Action { implicit request =>
    Ok("")
  }
  
  def upload = silhouette.SecuredAction(parse.multipartFormData) { implicit request =>
    request.body
      .file("picture")
      .map { picture =>
        
        val content = Files.readAllBytes(picture.ref.path)
      
        FormatDetector.detect(content) match {
          case Some(format) => {
            val hash =  MessageDigest.getInstance("SHA-256").digest(content)
    
            models.Picture(id=hash, uploadedAt=new DateTime(), format=format, content=content)
            
            Ok("")
          }
          case None => BadRequest("Unknown image format.")
        }
      }
      .getOrElse {
        BadRequest("Missing file.")
      }
  }
}
