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
import play.api.libs.json.Json
import play.api.mvc._

import daos.CustomColumnTypes
import forms.studios.SearchForm
import models.Studio

@Singleton
class StudiosController @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc)
  with CustomColumnTypes {

  import profile.api._

  def index = silhouette.UserAwareAction { implicit request =>
    Ok(views.html.studios.index(identity=request.identity))
  }

  def search = silhouette.UserAwareAction { implicit request =>
    SearchForm.form.bindFromRequest.fold(
      form => BadRequest("Invalid search parameters."),
      data => {
        println(data)
        Ok(Json.obj("results" -> Json.arr()))
      }
    )
  }

  def show(id: Studio#Id) = silhouette.UserAwareAction.async {
    implicit request =>

    db.run {
      daos.studioPicture.getStudioWithPictures(id)
    }.map {
      case (Some(studio), picIds) => {
        Ok(views.html.studios.show(
          identity = request.identity,
          studio, picIds))
      }
      case (None, _) => NotFound("Studio not found.")
    }
  }
}
