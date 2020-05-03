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

package controllers.account

import java.time.ZoneId
import javax.inject._

import scala.concurrent.Future

import play.api._
import play.api.mvc._

import forms.account.StudioForm
import models.{ PaymentPolicy, Studio }
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }

@Singleton
class StudiosController @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc) {

  import profile.api._

  /** Lists all studios from a single host. */
  def index = silhouette.SecuredAction.async { implicit request =>

    val user = request.identity.user

    db.run({ for {
      studios <- daos.studio.query.
        filter(_.ownerId === user.id).
        result

      picIds <- DBIO.sequence(
          studios.map { studio =>
            daos.studioPicture.
              withStudioPictureIds(studio.id).
              take(1).result.
              headOption
          })
      } yield studios zip picIds
    }.transactionally).map { studios =>
      Ok(views.html.account.studios(request.identity, studios))
    }
  }

  /** Shows a form to list a new studio. */
  def create = silhouette.SecuredAction { implicit request =>
    Ok(views.html.account.studioCreate(request.identity, StudioForm.form))
  }

  def createSubmit = silhouette.SecuredAction.async { implicit request =>

    StudioForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.account.studioCreate(
        request.identity, form))),
      data => {
        val timezone: ZoneId = timeZoneService.
          query(data.location.coordinates.lat, data.location.coordinates.long).
          getOrElse(ZoneId.of("UTC"))

        val studio: Future[Studio] = db.run({
          for {
            studio <- daos.studio.insert(Studio(
              ownerId = request.identity.user.id,
              name = data.name,
              description = data.description,
              location = data.location,
              timezone = timezone,
              openingSchedule = data.openingSchedule,
              pricingPolicy = data.pricingPolicy,
              bookingPolicy = data.bookingPolicy,
              paymentPolicy = data.paymentPolicy))
            _ <- daos.studioEquipment.setStudioEquipments(
              studio.id, data.equipments)
            _ <- daos.studioPicture.setStudioPics(studio.id, data.pictures)
          } yield studio
        }.transactionally)

        studio.map(s => Ok(s.toString))
      })
  }
}
