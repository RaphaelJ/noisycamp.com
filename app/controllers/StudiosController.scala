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

import play.api._
import play.api.mvc._

import daos.CustomColumnTypes
import models.Studio

@Singleton
class StudiosController @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc)
  with CustomColumnTypes {

  import profile.api._

  def index = silhouette.UserAwareAction.async { implicit request =>
    getClientConfig.map { clientConfig =>
      Ok(views.html.studios.index(clientConfig, user=request.identity))
    }
  }

  def show(id: Studio#Id) = silhouette.UserAwareAction.async {
    implicit request =>

    getClientConfig.onComplete(println _)

    for {
      clientConfig <- getClientConfig

      dbStudio <- db.run {
        for {
          studio <- daos.studio.query.
            filter(_.id === id).
            result.headOption

          picIds <- daos.studioPicture.query.
            filter(_.studioId === id).
            sortBy(_.id).
            map(_.pictureId).
            result
        } yield (studio, picIds)
      }
    } yield dbStudio match {
      case (Some(studio), picIds) => Ok(
        views.html.studios.show(
          clientConfig=clientConfig, user=request.identity, studio, picIds))
      case (None, _) => NotFound("Studio not found.")
    }
  }
}
