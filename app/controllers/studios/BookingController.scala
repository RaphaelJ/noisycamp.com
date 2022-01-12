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
import java.time.{ Duration, Instant, LocalDateTime }
import java.time.format.DateTimeFormatter

import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.util.ByteString
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.sendgrid.Response
import com.stripe.model.{ checkout, PaymentIntent }
import play.api._
import play.api.data.Form
import play.api.mvc._

import auth.DefaultEnv
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import daos.CustomColumnTypes
import forms.studios.BookingForm
import misc.StripePaymentCaptureMethod
import models.{ BookingDurations, BookingTimes, HasBookingTimes, CancellationPolicy, Equipment,
    Identity, LocalEquipment, LocalPricingPolicy, PaymentMethod, Picture, PriceBreakdown, Studio,
    StudioBooking, StudioCustomerBooking, StudioBookingEquipment, StudioBookingPaymentOnline,
    StudioBookingPaymentOnsite, StudioBookingStatus, User }
import java.time.Instant

@Singleton
class BookingController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc)
    with CustomColumnTypes {

    import profile.api._

    /** Shows a booking review page. */
    def show(id: Studio#Id) = SecuredAction.async { implicit request =>

        val now = Instant.now

        withStudioTransaction(id, { case (studio, owner, equips, picIds) =>
            validateAvailabilities(
                studio, BookingForm.form(now, studio, equips).bindFromRequest(request.queryString)).
                map { form =>
                    form.fold(
                        form => Left(form.errors),
                        data => {
                            val bookingTimes = data.bookingTimes

                            val localEquipments = data.equipments.
                                filter(_.price.isDefined).
                                map(_.localEquipment(studio))

                            val priceBreakdown = PriceBreakdown(
                                studio, bookingTimes.repeatOnce, localEquipments, None)

                            Right((bookingTimes, localEquipments, priceBreakdown))
                        })
                }.
                flatMap { summary =>
                    daos.user.query.
                        filter(_.id === studio.ownerId).
                        result.
                        head.
                        map { owner =>
                            Ok(views.html.studios.bookingReview(
                                identity = request.identity, owner, studio, picIds, summary))
                        }
                }
        })
    }

    /** Processes a reviewed booking. */
    def submit(id: Studio#Id) = SecuredAction.async { implicit request =>

        val now = Instant.now

        withStudioTransaction(id, { case (studio, owner, equips, picIds) =>
            validateAvailabilities(
                studio, BookingForm.formWithPaymentMethod(now, studio, equips).bindFromRequest).
                flatMap { form =>
                    form.fold(
                        form => {
                            DBIO.successful(Ok(views.html.studios.bookingReview(
                                identity = request.identity, owner, studio, picIds,
                                Left(form.errors))))
                        },
                        data => {
                            val handler = data.paymentMethod match {
                                case PaymentMethod.Online => handleOnlinePayment _
                                case PaymentMethod.Onsite => handleOnsitePayment _
                            }

                            val localEquipments = data.equipments.
                                filter(_.price.isDefined).
                                map(_.localEquipment(studio))

                            handler(
                                request.identity, studio, owner, picIds, data.bookingTimes,
                                localEquipments)
                        }
                    )
                }
        })
    }

    private def handleOnlinePayment(
        identity: Identity, studio: Studio, owner: User, pictures: Seq[Picture#Id],
        bookingTimes: BookingTimes, equipments: Seq[LocalEquipment])(
        implicit request: RequestHeader) : DBIO[Result] = {

        val user = identity.user

        val title: String = studio.name

        val dateStr = bookingTimes.beginsAt.toLocalDate.format(
            DateTimeFormatter.ofPattern("EEE MMM d, yyyy"))
        val dateStrShort = bookingTimes.beginsAt.toLocalDate.format(
            DateTimeFormatter.ofPattern("MMM d yyyy"))

        val description = f"Booking on $dateStr"

        val statement = f"NoisyCamp $dateStrShort"
        assert(statement.length <= 22)

        val pricingPolicy = studio.pricingPolicy
        val localPricingPolicy = studio.localPricingPolicy

        val onSuccessEscaped = routes.BookingController.paymentSuccess(
            studio.id, "{CHECKOUT_SESSION_ID}")
        // De-escape { and } characters
        val onSuccess = onSuccessEscaped.copy(
            url=onSuccessEscaped.url.replaceAll("%7B", "{").replaceAll("%7D", "}"))

        val beginsAt = bookingTimes.beginsAt
        val onCancel = _root_.controllers.routes.StudiosController.show(studio.URLId)

        val transactionFeeRate = Some(owner.plan.transactionRate)
        val priceBreakdown = PriceBreakdown(
            studio, bookingTimes.repeatOnce, equipments, transactionFeeRate)

        val metadata = Map(
            "charge_type" -> "booking",
        )

        for {
            session <- DBIO.from(paymentService.createSession(
                user, owner, priceBreakdown, title, description, statement, pictures,
                StripePaymentCaptureMethod.Manual, onSuccess, onCancel, metadata))

            sessionId = session.getId
            intentId = session.getPaymentIntent
            payment = StudioBookingPaymentOnline(sessionId, intentId)

            booking <- daos.studioBooking.
                insert(StudioCustomerBooking(
                    studio, user, StudioBookingStatus.PaymentProcessing,
                    studio.bookingPolicy.cancellationPolicy, bookingTimes.repeatOnce,
                    priceBreakdown, payment))

            bookingEquips <- daos.studioBookingEquipment.
                insert ++= equipments.map { e =>
                    StudioBookingEquipment(
                        bookingId = booking.id,
                        equipmentId = e.id)
                }

        } yield Ok(views.html.stripeCheckoutRedirect(identity = Some(identity), session))
    }

    private def handleOnsitePayment(
        identity: Identity, studio: Studio, owner: User, pictures: Seq[Picture#Id],
        bookingTimes: BookingTimes, equipments: Seq[LocalEquipment])(
        implicit request: RequestHeader) : DBIO[Result] = {

        val user = identity.user

        val status =
            if (studio.bookingPolicy.automaticApproval) {
                StudioBookingStatus.Valid
            } else {
                StudioBookingStatus.PendingValidation
            }

        val booking = StudioCustomerBooking(
            studio, user, status, studio.bookingPolicy.cancellationPolicy,
            bookingTimes.repeatOnce, equipments,
            None, StudioBookingPaymentOnsite())

        def onSuccess(booking: StudioCustomerBooking, owner: User) = {
            sendBookingEmails(booking, user, studio, pictures, owner, equipments).
                map { _ =>
                    Redirect(_root_.controllers.account.routes.BookingsController.show(booking.id)).
                        flashing("success" -> "Your session has been successfully booked.")
                }
        }

        for {
            booking <- daos.studioBooking.insert(booking)

            bookingEquips <- daos.studioBookingEquipment.
                insert ++= equipments.map { e =>
                    StudioBookingEquipment(
                        bookingId = booking.id,
                        equipmentId = e.id)
                    }

            // TODO: Do not send the email within the DB transaction.
            result <- DBIO.from { onSuccess(booking, owner)  }
        } yield result
    }

    /** Processes a valid booking payment redirect (from Stripe). */
    def paymentSuccess(studioId: Long, sessionId: String) = SecuredAction.async {
        implicit request =>

        def onPaymentSuccess(booking: StudioBooking) = {
            Redirect(_root_.controllers.account.routes.BookingsController.show(booking.id)).
                flashing("success" -> "Your session has been successfully booked.")
        }

        def onPaymentFailure(booking: StudioBooking) = {
            Redirect(_root_.controllers.routes.StudiosController.show(booking.studioId.toString)).
                flashing("error" ->
                    ("A problem occured during the processing of your payment. Please try again " +
                    "later."))
        }

        lazy val onBookingNotFound = NotFound("Booking not found.")

        db.run {
            daos.studioBooking.query.
                filter(_._1.studioId === studioId).
                filter(_._2.flatMap(_.stripeCheckoutSessionId.map(_ === sessionId))).
                result.
                headOption
        }.
            flatMap {
                case Some(booking) => {
                    booking.status match {
                        case StudioBookingStatus.PaymentProcessing => {
                            // It seems like we didn't receive the webhook notification from Stripe.
                            // Complete the payment here.
                            paymentService.retrieveSession(sessionId).
                                flatMap { session =>
                                    handleCheckoutSessionCompleted(
                                        session,
                                        onPaymentSuccess, onPaymentFailure, onBookingNotFound)
                                }

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

    /** Processes a finalized Stripe Checkout session, and tries to capture the charge.
     *
     * Runs and returns one of the provided result generator function, depending on the validaty
     * of the payment. */
    def handleCheckoutSessionCompleted(
        session: checkout.Session,
        onPaymentSuccess: StudioBooking => Result,
        onPaymentFailure: StudioBooking => Result,
        onBookingNotFound: Result)(
        implicit request: RequestHeader):
        Future[Result] = {

        // Sets the payment status as failed and uncapture the charge when possible
        def abortCharge(intent: PaymentIntent, booking: StudioBooking) = {
            DBIO.from(
                if (intent.getStatus == "requires_capture") {
                    paymentService.cancelPaymentIntent(intent)
                } else {
                    Future.successful(intent)
                }
            ).andThen {
                daos.studioBooking.query.
                    filter(_._1.id === booking.id).
                    map(_._1.status).
                    update(StudioBookingStatus.PaymentFailure).
                    map { _ => onPaymentFailure(booking) }
            }
        }

        // Tries to capture the charge and change to booking status to valid or pending-validation.
        def processCharge(intent: PaymentIntent, studio: Studio, booking: StudioCustomerBooking) = {
            (intent.getStatus match {
                case "requires_capture" => {
                    DBIO.from(paymentService.capturePaymentIntent(intent)).
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
                        val newBooking = booking.copy(status = newStatus)

                        for {
                            _ <- daos.studioBooking.bookingQuery.
                                filter(_.id === newBooking.id).
                                map(_.status).
                                update(newStatus)

                            customer <- daos.user.query.
                                filter(_.id === newBooking.customerId).
                                result.
                                head

                            owner <- daos.user.query.
                                filter(_.id === studio.ownerId).
                                result.
                                head

                            pictures <- daos.studioPicture.
                                withStudioPictureIds(studio.id).
                                result

                            equips <- daos.studioBookingEquipment.
                                withBookingEquipment(booking.id).
                                result.
                                map(_.map(_.localEquipment(studio)))

                            _ <- DBIO.from(
                                sendBookingEmails(
                                    newBooking, customer, studio, pictures, owner, equips))
                        } yield onPaymentSuccess(newBooking)
                    }
                    case false => abortCharge(intent, booking)
                }
        }

        for {
            intent <- paymentService.retrievePaymentIntent(session.getPaymentIntent)

            res <- db.run({
                daos.studioBooking.query.
                    filter { case (b, scb, _) =>
                        scb.flatMap(_.stripeCheckoutSessionId.map(_ === session.getId)) }.
                    join(daos.studio.query).
                        on(_._1.studioId === _.id).
                    result.
                    headOption.
                    flatMap {
                        case Some((booking, studio)) => {
                            val scb = (booking: StudioBooking).asInstanceOf[StudioCustomerBooking]

                            scb.status match {
                                case StudioBookingStatus.PaymentProcessing => {
                                    // Validates the charge and the booking if we can still book the
                                    // requested times.

                                    daos.studioBooking.
                                        hasOverlap(studio, scb.times).
                                        flatMap {
                                            case true => abortCharge(intent, booking)
                                            case false => processCharge(intent, studio, scb)
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
        f: ((Studio, User, Seq[Equipment], Seq[Picture#Id])
            => DBIOAction[Result, NoStream, Effect.All]))
        (implicit request: SecuredRequest[DefaultEnv, T]): Future[Result] = {

        val user = request.identity.user

        db.run({
            for {
                studioOpt <- daos.studio.query.
                    filter(_.id === id).
                    join(daos.user.query).on(_.ownerId === _.id).
                    result.headOption

                equips <- daos.studioEquipment.withStudioEquipment(id).result
                picIds <- daos.studioPicture.withStudioPictureIds(id).result

                result <- studioOpt match {
                    case Some((studio, owner)) if studio.canAccess(Some(user)) => {
                        f(studio, owner, equips, picIds)
                    }
                    case _ => DBIO.successful(NotFound("Studio not found."))
                }
            } yield result //: DBIOAction[Result, NoStream, Effect.All]
        }.transactionally)
    }

    /** Validates the booking time of a form. Adds a global FormError if the studio is not
     * available during the requested times. */
    private def validateAvailabilities[PM](
        studio: Studio,
        form: Form[BookingForm.DataGeneric[PM]])
        : DBIO[Form[BookingForm.DataGeneric[PM]]] = {

        if (form.hasErrors) {
            DBIO.successful(form)
        } else {
            val times = form.get.bookingTimes.repeatOnce

            daos.studioBooking.
                hasOverlap(studio, times).
                map {
                    case true => form.withGlobalError(
                        "The studio is not available during the selected booking period.")
                    case false => form
                }
        }
    }

    private def sendBookingEmails(
        booking: StudioCustomerBooking, customer: User, studio: Studio, pictures: Seq[Picture#Id],
        owner: User, equips: Seq[LocalEquipment])(
        implicit request: RequestHeader, config: Configuration):
        Future[(Response, Response)] = {

        if (booking.isAccepted) {
            // Automatically accepted. Confirm the booking to both actors.
            emailService.sendBookingAccepted(booking, customer, studio, pictures, owner, equips).
                zip(emailService.sendBookingReceived(booking, customer, studio, owner, equips))
        } else {
            // Booking required review. Notifies the customer and sends the request to the
            // owner.
            emailService.sendBookingRequestInReview(booking, customer, studio).
                zip(emailService.sendBookingRequest(booking, customer, studio, owner, equips))
        }
    }
}
