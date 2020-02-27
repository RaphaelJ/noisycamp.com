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
import java.time.{ Duration, LocalDateTime }
import java.time.format.DateTimeFormatter

import scala.concurrent.Future
import scala.util.{ Success, Failure }

import play.api._
import play.api.mvc._

import _root_.controllers.{ ClientConfig, CustomBaseController,
  CustomControllerCompoments }
import daos.CustomColumnTypes
import forms.studios.{ BookingForm, BookingTimesForm }
import misc.PaymentCaptureMethod
import models.{ BookingTimes, LocalPricingPolicy, PaymentMethod, Picture,
  Studio, StudioBooking, StudioBookingPaymentOnline, StudioBookingStatus, User }

@Singleton
class Booking @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc)
  with CustomColumnTypes {

  import profile.api._

  /** Shows a booking review page. */
  def show(id: Studio#Id, beginsAt: String, duration: Int)
    = silhouette.SecuredAction.async { implicit request =>

    withStudio(id, { case (studio, picIds) =>
      val params = Map(
        "begins-at"       -> beginsAt,
        "duration"        -> duration.toString)

      val summary = BookingTimesForm.form(studio).bind(params).fold(
          form => Left(form.errors),
          bookingTimes => Right(bookingTimes)
        )

      DBIO.successful(Ok(views.html.studios.booking(
        user = Some(request.identity),
        studio, picIds, summary)))
    })
  }

  def submit(id: Studio#Id)
    = silhouette.SecuredAction.async {
    implicit request =>

    withStudio(id, { case (studio, picIds) =>
      BookingForm.form(studio).bindFromRequest.fold(
        form => DBIO.successful(BadRequest("Invalid booking request.")),
        data => {
          val handler = data.paymentMethod match {
            case PaymentMethod.Online => handleOnlinePayment _
            case PaymentMethod.Onsite => handleOnsitePayment _
          }

          DBIO.from(handler(request.identity, studio, picIds,
            data.bookingTimes))
        }
      )
    })
  }

  def paymentSuccess(id: Long, sessionId: String) =
    silhouette.SecuredAction.async { implicit request =>

    lazy val onPaymentSuccess = Ok("Payment successful")
    val onPaymentFailure = { booking: StudioBooking =>
        Forbidden("Payment failed.")
    }
    lazy val onBookingNotFound = NotFound("Booking not found.")

    for {
      session <- paymentService.retreiveSession(sessionId)
      intent <- paymentService.retreiveIntent(session.getPaymentIntent)

      res <- db.run({
        daos.studioBooking.query.
          filter(_.stripeCheckoutSessionId === sessionId).
          result.
          headOption.
          flatMap {
            case Some(booking) => for {
              chargedSuccessfully <- intent.getStatus match {
                case "requires_capture" =>
                  DBIO.from(paymentService.capturePayment(intent)).
                    map(_ => true)
                case "succeeded" => DBIO.successful(true)
                case _ => DBIO.successful(false)
              }

              res <-
                if (chargedSuccessfully) {
                  daos.studioBooking.query.
                    filter(_.stripeCheckoutSessionId === sessionId).
                    map(_.status).
                    update(StudioBookingStatus.Succeeded).
                    map { _ => onPaymentSuccess }
                } else {
                  DBIO.successful(onPaymentFailure(booking))
                }
            } yield res

            case None => DBIO.successful(onBookingNotFound)
          }
      }.transactionally)

    } yield res
  }

  def stripeCompleted = silhouette.UserAwareAction {
    Ok("")
  }

  /** Executes the function within the DBIO monad, or returns a 404 response. */
  private def withStudio[T](id: Studio#Id,
    f: ((Studio, Seq[Picture#Id]) => DBIOAction[Result, NoStream, Effect.All]))
    (implicit request: Request[T]):
    Future[Result] = {

    db.run {
      val dbStudio = daos.studioPicture.getStudioWithPictures(id)

      dbStudio.flatMap {
        case (Some(studio), picIds) => f(studio, picIds)
        case (None, _) => DBIO.successful(NotFound("Studio not found."))
      }: DBIOAction[Result, NoStream, Effect.All]
    }
  }

  private def isBookingPossible(studio: Studio, bookingTimes: BookingTimes):
    DBIO[Boolean] = {

    DBIO.successful(false)
    // val schedule =
    //
    // if (!studio.openingSchedule.isStudioOpen(bookingTimes)) {
    //   DBIO.successful(false)
    // } else {
    //
    // }
    //
    // daos.studioBooking.query.
    //   filter()

  }

  private def handleOnlinePayment(user: User, studio: Studio,
    studioPics: Seq[Picture#Id], bookingTimes: BookingTimes)(
    implicit request: RequestHeader) : Future[Result] = {

    val title: String = studio.name

    val dateStr = bookingTimes.beginsAt.toLocalDate.format(
      DateTimeFormatter.ofPattern("EEE MMM d, yyyy"))
    val description = f"Book on $dateStr"
    val statement = f"NoisyCamp booking"

    val pricingPolicy = studio.localPricingPolicy

    // TODO: compute according to booking times and cu
    val total = pricingPolicy.pricePerHour

    val onSuccessEscaped = routes.Booking.paymentSuccess(
      studio.id, "{CHECKOUT_SESSION_ID}")
    // De-escape { and } characters
    val onSuccess = onSuccessEscaped.copy(
      url=onSuccessEscaped.url.replaceAll("%7B", "{").replaceAll("%7D", "}"))

    val beginsAt = bookingTimes.beginsAt
    val onCancel = routes.Booking.show(
      id = studio.id,
      beginsAt = beginsAt.toString,
      duration = bookingTimes.duration.getSeconds.toInt)

    val stripeSession = paymentService.initiatePayment(
      user, total, title, description, statement, studioPics,
      PaymentCaptureMethod.Manual, onSuccess, onCancel)

    stripeSession.flatMap { sess =>

      val sessionId = sess.getId
      val intentId = sess.getPaymentIntent
      val payment = StudioBookingPaymentOnline(sessionId, intentId)

      val booking = db.run({
        daos.studioBooking.insert(StudioBooking(
          studioId = studio.id,
          customerId = user.id,
          status = StudioBookingStatus.Processing,
          beginsAt = bookingTimes.beginsAt,
          durationTotal = bookingTimes.duration,
          durationRegular = bookingTimes.duration,
          durationEvening = Duration.ZERO,
          durationWeekend = Duration.ZERO,
          currency = total.currency,
          total = total.amount,
          regularPricePerHour = pricingPolicy.pricePerHour.amount,
          eveningPricePerHour =
            pricingPolicy.evening.map(_.pricePerHour.amount),
          weekendPricePerHour =
            pricingPolicy.weekend.map(_.pricePerHour.amount),
          payment = payment))
      }.transactionally)

      booking.map { _ =>
        Ok(views.html.studios.bookingCheckout(user = Some(user), sess))
      }
    }
  }

  private def handleOnsitePayment(user: User, studio: Studio,
    studioPics: Seq[Picture#Id], bookingTimes: BookingTimes)(
    implicit request: RequestHeader) : Future[Result] = {

    Future.successful(Ok(bookingTimes.toString))
  }
}
