/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2021  Raphael Javaux <raphael@noisycamp.com>
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

package controllers.account.studios

import javax.inject._

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api._
import play.api.mvc._

import models.{ Studio }
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }

@Singleton
class EmbeddedController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc) {

    import profile.api._

    def index(id: Studio#Id) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run {
            for {
                studio <- daos.studio.query.
                    filter(_.id === id).
                    result.headOption
            } yield studio
        }.map {
            case Some(studio) if studio.isOwner(user) => {
                Ok(views.html.account.studios.embedded.index(request.identity, studio))
            }
            case Some(_) => Forbidden("Only the studio owner can edit settings.")
            case None => NotFound("Studio not found.")
        }
    }

    def demo(id: Studio#Id) = SecuredAction.async { implicit request =>
        db.run({ for {
            studioOpt <- daos.studio.query.
                filter(_.id === id).
                result.headOption

            picIds <- daos.studioPicture.withStudioPictureIds(id).result
        } yield {
            studioOpt match {
                case Some(studio) => {
                    Ok(views.html.account.studios.embedded.demo(studio, picIds))
                }
                case _ => NotFound("Studio not found.")
            }
        } }.transactionally)
    }
}
