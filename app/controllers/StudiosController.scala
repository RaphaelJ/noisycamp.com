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

import java.time.DayOfWeek
import javax.inject._
import scala.concurrent.Future

import play.api._
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc._

import daos.CustomColumnTypes
import forms.studios.SearchForm
import misc.GIS
import misc.JsonWrites._
import models.{ BBox, Studio, StudioWithPicture }

@Singleton
class StudiosController @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc)
  with CustomColumnTypes {

  import profile.api._

  def index = silhouette.UserAwareAction { implicit request =>
    Ok(views.html.studios.index(identity=request.identity))
  }

  def search = silhouette.UserAwareAction.async { implicit request =>
    SearchForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest("Invalid search parameters.")),
      data => {
        // Uses the BBox parameter, or generates a BBox around the location's
        // center if not provided.
        val bboxRadius = BigDecimal(60 * 1000) // 60 Km
        val bboxOpt = data.bbox.
          orElse { data.center.map(GIS.centeredBBox(bboxRadius, _)) }

        db.run({ for {
          // Fetches studios matching query.
          studios <- daos.studio.query.
            filter { studio =>
              // Geographical filter
              val isInBBox = bboxOpt match {
                case Some(BBox(north, south, west, east)) => {
                  studio.long >= west && studio.long <= east &&
                  studio.lat >= south && studio.lat <= north
                }
                case None => true: Rep[Boolean]
              }

              // Is open on date
              val isOpen = data.availableOn match {
                case Some(date) => {
                  date.getDayOfWeek match {
                    case DayOfWeek.MONDAY => studio.mondayIsOpen
                    case DayOfWeek.TUESDAY => studio.tuesdayIsOpen
                    case DayOfWeek.WEDNESDAY => studio.wednesdayIsOpen
                    case DayOfWeek.THURSDAY => studio.thursdayIsOpen
                    case DayOfWeek.FRIDAY => studio.fridayIsOpen
                    case DayOfWeek.SATURDAY => studio.saturdayIsOpen
                    case DayOfWeek.SUNDAY => studio.sundayIsOpen
                  }
                }
                case None => true: Rep[Boolean]
              }

              // FIXME: does not filter out studios that are not availaible on
              // the selected day.

              isInBBox && isOpen
            }.
            take(200). // Limits to 200 studios
            result

          // Fetches matching studios' pictures
          picIds <- DBIO.sequence(
            studios.map { studio =>
              daos.studioPicture.
                withStudioPictureIds(studio.id).
                take(1).
                result.
                headOption
              })

          } yield studios zip picIds

        }.transactionally).map { studios =>

          val studiosWithPicture = studios.
            map { case (studio, picId) => StudioWithPicture(studio, picId) }
          Ok(Json.obj("results" -> Json.toJson(studiosWithPicture)))
        }
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
