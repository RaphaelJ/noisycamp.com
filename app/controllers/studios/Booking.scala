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

package controllers.studios

import javax.inject._
import java.time.{ Duration, LocalDate, LocalTime }

import scala.concurrent.Future

import play.api._
import play.api.mvc._

import _root_.controllers.{ ClientConfig, CustomBaseController,
  CustomControllerCompoments }
import daos.CustomColumnTypes
import forms.studios.{ BookingForm, BookingTimesForm }
import models.{ BookingTimes, LocalPricingPolicy, PaymentMethod, Picture,
  Studio }

@Singleton
class Booking @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc)
  with CustomColumnTypes {

  import profile.api._

  def show(id: Studio#Id, date: String, time: String, duration: Int)
    = silhouette.SecuredAction.async { implicit request =>

    withStudio(id, { case (clientConfig, studio, picIds, pricingPolicy) =>
      val params = Map(
        "date"            -> date,
        "time"            -> time,
        "duration"        -> duration.toString)

      BookingTimesForm.form.bind(params).fold(
        form => DBIO.successful(BadRequest("Invalid booking request.")),
        bookingTimes => {
          DBIO.successful(Ok(
            views.html.studios.book(
              clientConfig = clientConfig,
              user = Some(request.identity),
              studio, pricingPolicy, picIds, bookingTimes)))
        }
      )
    })
  }

  def submit(id: Studio#Id)
    = silhouette.SecuredAction.async {
    implicit request =>

    withStudio(id, { case (clientConfig, studio, picIds, pricingPolicy) =>
      BookingForm.form.bindFromRequest.fold(
        form => DBIO.successful(BadRequest("Invalid booking request.")),
        data => {
          val handler = data.paymentMethod match {
            case PaymentMethod.Online => handleOnlinePayment _
            case PaymentMethod.Onsite => handleOnsitePayment _
          }

          DBIO.from(handler(studio, pricingPolicy, data))
        }
      )
    })
  }

  /** Executes the function within the DBIO monad, or returns a 404 response. */
  private def withStudio[T](id: Studio#Id,
    f: (
      (ClientConfig, Studio, Seq[Picture#Id], LocalPricingPolicy)
      => DBIOAction[Result, NoStream, Effect.All]))
    (implicit request: Request[T]):
    Future[Result] = {

    getClientConfig.flatMap { clientConfig =>
      db.run {
        val dbStudio = daos.studioPicture.getStudioWithPictures(id)

        dbStudio.flatMap {
          case (Some(studio), picIds) => {
            val pricingPolicy = studio.
              localPricingPolicy.
              in(clientConfig.currency)(exchangeRatesService.exchangeRates)

            f(clientConfig, studio, picIds, pricingPolicy)
          }
          case (None, _) => DBIO.successful(NotFound("Studio not found."))
        }: DBIOAction[Result, NoStream, Effect.All]
      }
    }
  }

  private def handleOnlinePayment(studio: Studio,
    pricingPolicy: LocalPricingPolicy, formData: BookingForm.Data)
    : Future[Result] = {

    Future.successful(Ok(formData.toString))
  }

  private def handleOnsitePayment(studio: Studio,
    pricingPolicy: LocalPricingPolicy, formData: BookingForm.Data)
    : Future[Result] = {

    Future.successful(Ok(formData.toString))
  }
}
