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

import akka.util.ByteString
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.stripe.model.checkout
import com.stripe.model.PaymentIntent
import play.api._
import play.api.data.Form
import play.api.mvc._

import auth.DefaultEnv
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import daos.CustomColumnTypes
import forms.studios.{ BookingForm, BookingTimesForm }
import misc.PaymentCaptureMethod
import models.{ BookingDurations, BookingTimes, CancellationPolicy, HasBookingTimes, Identity,
    LocalPricingPolicy, PaymentMethod, Picture, PriceBreakdown, Studio, StudioBooking,
    StudioBookingPaymentOnline, StudioBookingPaymentOnsite, StudioBookingStatus, User }

@Singleton
class BookingController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc)
    with CustomColumnTypes {

    import profile.api._

    /** Shows a booking review page. */
    def show(id: Studio#Id, beginsAt: String, duration: Int)
        = silhouette.SecuredAction.async { implicit request =>

        val params = Map(
            "begins-at"       -> beginsAt,
            "duration"        -> duration.toString)

        withStudioTransaction(id, { case (studio, picIds) =>
            validateAvailabilities(studio, BookingTimesForm.form(studio).bind(params)).
                map { form =>
                    form.fold(
                        form => Left(form.errors),
                        bookingTimes => {
                            // Computes the price components based on the booked times.
                            val pricingPolicy = studio.pricingPolicy
                            val localPricingPolicy = studio.localPricingPolicy
                            val bookingDurations = studio.openingSchedule.
                                validateBooking(pricingPolicy, bookingTimes).
                                get
                            val priceBreakdown = PriceBreakdown(
                                bookingDurations, localPricingPolicy.pricePerHour,
                                localPricingPolicy.evening.map(_.pricePerHour),
                                localPricingPolicy.weekend.map(_.pricePerHour), None)

                            Right((bookingTimes, priceBreakdown))
                        })
                }.
                flatMap { summary =>
                    daos.user.query.
                        filter(_.id === studio.ownerId).
                        result.
                        head.
                        map { owner => 
                            Ok(views.html.studios.booking(
                                identity = request.identity, owner, studio, picIds, summary))
                        }
                }
        })
    }

    /** Processes a reviewed booking. */
    def submit(id: Studio#Id) = silhouette.SecuredAction.async { implicit request =>

        withStudioTransaction(id, { case (studio, picIds) =>
            validateAvailabilities(studio, BookingForm.form(studio).bindFromRequest).
                flatMap { form =>
                    form.fold(
                        form => {
                            daos.user.query.
                                result.
                                head.
                                map { owner => 
                                    Ok(views.html.studios.booking(
                                        identity = request.identity, owner, studio, picIds,
                                        Left(form.errors)))
                                }
                        },
                        data => {
                            val handler = data.paymentMethod match {
                                case PaymentMethod.Online => handleOnlinePayment _
                                case PaymentMethod.Onsite => handleOnsitePayment _
                            }

                            handler(request.identity, studio, picIds, data.bookingTimes)
                        }
                    )
                }
        })
    }

    private def handleOnlinePayment(identity: Identity, studio: Studio, studioPics: Seq[Picture#Id],
        bookingTimes: BookingTimes)(implicit request: RequestHeader) : DBIO[Result] = {

        val user = identity.user

        val title: String = studio.name

        val dateStr = bookingTimes.beginsAt.toLocalDate.format(
            DateTimeFormatter.ofPattern("EEE MMM d, yyyy"))
        val description = f"Book on $dateStr"
        val statement = f"NoisyCamp booking"

        val pricingPolicy = studio.pricingPolicy
        val localPricingPolicy = studio.localPricingPolicy

        val onSuccessEscaped = routes.BookingController.paymentSuccess(
            studio.id, "{CHECKOUT_SESSION_ID}")
        // De-escape { and } characters
        val onSuccess = onSuccessEscaped.copy(
            url=onSuccessEscaped.url.replaceAll("%7B", "{").replaceAll("%7D", "}"))

        val beginsAt = bookingTimes.beginsAt
        val onCancel = routes.BookingController.show(
            studioId = studio.id,
            beginsAt = beginsAt.toString,
            duration = bookingTimes.duration.getSeconds.toInt)

        for {
            owner <- daos.user.query.
                filter(_.id === studio.ownerId).
                result.
                head

            transactionFeeRate = Some(owner.plan.transactionRate)
            priceBreakdown = PriceBreakdown(studio, bookingTimes, transactionFeeRate)

            session <- DBIO.from(paymentService.initiatePayment(
                user, priceBreakdown, title, description, statement, studioPics,
                PaymentCaptureMethod.Manual, onSuccess, onCancel))

            sessionId = session.getId
            intentId = session.getPaymentIntent
            transactionFeeRate = Some(owner.plan.transactionRate)
            payment = StudioBookingPaymentOnline(sessionId, intentId)

            booking <- daos.studioBooking.
                insert(StudioBooking(
                    studio, user, StudioBookingStatus.PaymentProcessing,
                    studio.bookingPolicy.cancellationPolicy, bookingTimes, priceBreakdown,
                    payment))

        } yield Ok(views.html.studios.bookingCheckout(identity = Some(identity), session))
    }

    private def handleOnsitePayment(identity: Identity, studio: Studio, studioPics: Seq[Picture#Id],
        bookingTimes: BookingTimes)(implicit request: RequestHeader) : DBIO[Result] = {

        val user = identity.user

        val status =
            if (studio.bookingPolicy.automaticApproval) {
                StudioBookingStatus.Valid
            } else {
                StudioBookingStatus.PendingValidation
            }

        val booking = StudioBooking(
            studio, user, status, studio.bookingPolicy.cancellationPolicy, bookingTimes, None,
            StudioBookingPaymentOnsite())

        daos.studioBooking.
            insert(booking).
            map { booking =>
                Redirect(_root_.controllers.account.routes.BookingsController.show(booking.id)).
                    flashing("success" -> "Your session has been successfully booked.")
            }
    }

    /** Processes a valid booking payment redirect (from Stripe). */
    def paymentSuccess(studioId: Long, sessionId: String) = silhouette.SecuredAction.async {
        implicit request =>

        def onPaymentSuccess(booking: StudioBooking) = {
            Redirect(_root_.controllers.account.routes.BookingsController.show(booking.id)).
                flashing("success" -> "Your session has been successfully booked.")
        }

        def onPaymentFailure(booking: StudioBooking) = {
            Redirect(routes.BookingController.show(
                booking.studioId, booking.times.beginsAt.toString,
                booking.times.duration.getSeconds.toInt)).
                flashing("error" ->
                    "A problem occured during the processing of your payment. Please try  later.")
        }

        lazy val onBookingNotFound = NotFound("Booking not found.")

        db.run {
            daos.studioBooking.query.
                filter(_.studioId === studioId).
                filter(_.stripeCheckoutSessionId === sessionId).
                result.
                headOption
        }.
            flatMap {
                case Some(booking) => {
                    booking.status match {
                        case StudioBookingStatus.PaymentProcessing => {
                            // It seems like we didn't receive the webhook notification from Stripe.
                            // Complete the payment here.
                            handlePaymentCompleted(Right(sessionId), onPaymentSuccess,
                                onPaymentFailure, onBookingNotFound)
                        }
                        case StudioBookingStatus.PaymentFailure => {
                            Future.successful(onPaymentFailure(booking))
                        }
                        case _ => Future.successful(onPaymentSuccess(booking))
                    }
                }
                case None => Future.successful(onBookingNotFound)
            }
    }

    /** Receives a valid payment webhook notification from Stripe. */
    def stripeCompleted = Action(parse.byteString).async { request: Request[ByteString] =>

        paymentService.withWebhookEvent(request, { event =>
            if (event.getType == "checkout.session.completed") {
                val session = event.getDataObjectDeserializer.getObject.
                    get.
                    asInstanceOf[checkout.Session]

                val onPaymentSuccess = (_: StudioBooking) => Ok("payment-success")
                val onPaymentFailure = (_: StudioBooking) => Ok("payment-failure")
                val onBookingNotFound = NotFound("booking-not-found")

                handlePaymentCompleted(
                    Left(session), onPaymentSuccess, onPaymentFailure, onBookingNotFound)
            } else {
                Future.successful(NotFound("event-type-unknown"))
            }
        })
    }

    /** Processes a finalized Stripe Checkout session, and tries to capture the charge.
     *
     * Runs and returns one of the provided result generator function, depending on the validaty
     * of the payment. */
    private def handlePaymentCompleted(
        sessionOrId: /* Session or Session ID */ Either[checkout.Session, String],
        onPaymentSuccess: StudioBooking => Result,
        onPaymentFailure: StudioBooking => Result,
        onBookingNotFound: Result):
        Future[Result] = {

        // Sets the payment status as failed and uncapture the charge when possible
        def abortCharge(intent: PaymentIntent, booking: StudioBooking) = {
            DBIO.from(
                if (intent.getStatus == "requires_capture") {
                    paymentService.cancelPayment(intent)
                } else {
                    Future.successful(intent)
                }
            ).andThen {
                daos.studioBooking.query.
                    filter(_.id === booking.id).
                    map(_.status).
                    update(StudioBookingStatus.PaymentFailure).
                    map { _ => onPaymentFailure(booking) }
            }
        }

        // Tries to capture the charge and change to booking status to valid or pending-validation.
        def processCharge(intent: PaymentIntent, studio: Studio, booking: StudioBooking) = {
            (intent.getStatus match {
                case "requires_capture" => {
                    DBIO.from(paymentService.capturePayment(intent)).
                        map(_ => true)
                }
                case "succeeded" => DBIO.successful(true)
                case _ => DBIO.successful(false)
            }).
                flatMap {
                    case true => {
                        val newStatus =
                            if (studio.bookingPolicy.automaticApproval) {
                                StudioBookingStatus.Valid
                            } else {
                                StudioBookingStatus.PendingValidation
                            }

                        daos.studioBooking.query.
                            filter(_.id === booking.id).
                            map(_.status).
                            update(newStatus).
                            map { _ => onPaymentSuccess(booking) }
                    }
                    case false => abortCharge(intent, booking)
                }
        }

        for {
            session <- sessionOrId match {
                case Left(session) => Future.successful(session)
                case Right(sessionId) => paymentService.retreiveSession(sessionId)
            }

            intent <- paymentService.retreiveIntent(session.getPaymentIntent)

            res <- db.run({
                daos.studio.query.
                    join(daos.studioBooking.query).on(_.id === _.studioId).
                    filter { case (s, b) => b.stripeCheckoutSessionId === session.getId }.
                    result.
                    headOption.
                    flatMap {
                        case Some((studio, booking)) => {
                            booking.status match {
                                case StudioBookingStatus.PaymentProcessing => {
                                    // Validates the charge and the booking if we can still book the
                                    // requested times.

                                    daos.studioBooking.
                                        hasOverlap(studio, booking.times).
                                        flatMap {
                                            case true => abortCharge(intent, booking)
                                            case false => processCharge(intent, studio, booking)
                                        }
                                }
                                case StudioBookingStatus.PaymentFailure => {
                                    // For some reason, this booking's payment already failed,
                                    // tries to refund any uncaptured charge if possible.
                                    abortCharge(intent, booking)
                                }
                                case _ => DBIO.successful(onPaymentSuccess(booking))

                            }
                        }
                        case None => DBIO.successful(onBookingNotFound)
                    }
            }.transactionally)
        } yield res
    }

    /** Executes the function within the DBIO monad, or returns a 404 response. */
    private def withStudioTransaction[T](id: Studio#Id,
        f: ((Studio, Seq[Picture#Id]) => DBIOAction[Result, NoStream, Effect.All]))
        (implicit request: SecuredRequest[DefaultEnv, T]): Future[Result] = {

        val user = request.identity.user

        db.run({
            val dbStudio = daos.studioPicture.getStudioWithPictures(id)

            dbStudio.flatMap {
                case (Some(studio), picIds) if studio.canAccess(Some(user)) => f(studio, picIds)
                case _ => DBIO.successful(NotFound("Studio not found."))
            }: DBIOAction[Result, NoStream, Effect.All]
        }.transactionally)
    }

    /** Validates the booking time of a form. Adds a global FormError if the studio is not
     * available during the requested times. */
    private def validateAvailabilities[T <: HasBookingTimes](studio: Studio, form: Form[T])
        : DBIO[Form[T]] = {

        if (form.hasErrors) {
            DBIO.successful(form)
        } else {
            val times = form.get.bookingTimes

            daos.studioBooking.hasOverlap(studio, times).
                map {
                    case true => form.withGlobalError(
                        "The studio is not available during the selected booking period.")
                    case false => form
                }
        }
    }
}
