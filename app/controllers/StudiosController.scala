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
import scala.concurrent.ExecutionContext

import com.mohiva.play.silhouette.api.Silhouette
import play.api._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.mvc._
import slick.jdbc.JdbcProfile

import auth.DefaultEnv
import daos.{ CustomColumnTypes, StudioDAO, StudioPictureDAO }
import models.Studio

@Singleton
class StudiosController @Inject() (
  cc: ControllerComponents,
  implicit val config: Configuration,
  protected val dbConfigProvider: DatabaseConfigProvider,
  studioDao: StudioDAO,
  studioPictureDao: StudioPictureDAO,
  silhouette: Silhouette[DefaultEnv])
  (implicit executionContext: ExecutionContext)
  extends AbstractController(cc)
  with HasDatabaseConfigProvider[JdbcProfile]
  with CustomColumnTypes {

  import profile.api._

  def index = silhouette.UserAwareAction { implicit request =>
    Ok(views.html.studios.index(user=request.identity))
  }

  def show(id: Studio#Id) = silhouette.UserAwareAction.async {
    implicit request =>

    db.run {
      for {
        studio <- studioDao.query.
          filter(_.id === id).
          result.headOption

        picIds <- studioPictureDao.query.
          filter(_.studioId === id).
          map(_.pictureId).
          result
      } yield (studio, picIds)
    }.map {
      case (Some(studio), picIds) => Ok(
        views.html.studios.show(user=request.identity, studio, picIds))
      case (None, _) => NotFound("Studio not found.")
      }
  }
}
