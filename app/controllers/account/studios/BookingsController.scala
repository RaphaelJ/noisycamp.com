/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphaeljavaux@gmail.com>
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

import java.time.{ Instant, ZoneId }
import javax.inject._

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api._
import play.api.mvc._

import auth.DefaultEnv
import daos.{ CustomColumnTypes, StudioBookingDAO }
import daos.StudioBookingDAO.toStudioBooking
import daos.StudioBookingDAO.{
    StudioBookingRow, StudioCustomerBookingRow, StudioManualBookingRow, toStudioBooking }
import forms.account.ManualBookingForm
import models.{ Studio, StudioBooking, StudioCustomerBooking, StudioManualBooking,
    StudioBookingPaymentOnline, StudioBookingPaymentOnsite, StudioBookingStatus, User }
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import misc.PaymentService

@Singleton
class BookingsController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc)
    with CustomColumnTypes {

    import profile.api._

    def index(id: Studio#Id) = SecuredAction.async { implicit request =>
        withStudioBookings(id) { case (studio, bookings) =>
            Ok(views.html.account.studios.bookings.index(request.identity, studio, bookings))
        }
    }

    def calendar(id: Studio#Id) = SecuredAction.async { implicit request =>
        withStudioBookings(id, onlyActive=true) { case (studio, bookings) =>
            val bookingEvents = bookings.
                collect { case (b: StudioCustomerBooking, c) =>
                    b.toEvent(c).withClasses(Seq("booking")) }

            Ok(views.html.account.studios.bookings.calendar(
                request.identity, studio, bookingEvents))
        }
    }

    def calendarSync(id: Studio#Id) = SecuredAction { implicit request =>
        val user = request.identity.user

        if (user.plan.calendarSync) {
            Ok(views.html.featureNotYetImplemented(request.identity))
        } else {
            Redirect(_root_.controllers.account.routes.PremiumController.upgrade).
                flashing("error" ->
                    ("Upgrade to NoisyCamp Premium to synchronize NoisyCamp with your favorite " +
                    "calendar app."))
        }
    }

    def create(id: Studio#Id) = SecuredAction.async { implicit request =>
        val now = Instant.now

        ifUserHasManualBookings {
            withStudioTransaction(id) { studio =>
                val form = ManualBookingForm.form(now, studio)
                DBIO.successful(
                    Ok(views.html.account.studios.bookings.create(
                        request.identity, now, studio, form)))
            }
        }
    }

    def createSubmit(id: Studio#Id) = SecuredAction.async { implicit request =>
        val now = Instant.now

        ifUserHasManualBookings {
            withStudioTransaction(id) { case studio =>
                ManualBookingForm.
                    form(now, studio).
                    bindFromRequest.
                    fold(
                        form => DBIO.successful(BadRequest(
                            views.html.account.studios.bookings.create(
                                request.identity, now, studio, form))),
                        data => {
                            daos.studioBooking.insert(StudioManualBooking(
                                studio,
                                data.title,
                                StudioBookingStatus.Valid,
                                data.times.withRepeat(data.repeat))).
                                map { booking =>
                                    Redirect(routes.BookingsController.show(studio.id, booking.id)).
                                        flashing("success" -> "Booking successfully created.")
                                }
                        })
            }
        }
    }

    private def ifUserHasManualBookings[B](f: => Future[Result])(
        implicit request: SecuredRequest[DefaultEnv, B]): Future[Result] = {

        val user = request.identity.user

        if (user.plan.manualBookings) {
            f
        } else {
            Future.successful {
                Redirect(_root_.controllers.account.routes.PremiumController.upgrade).
                    flashing("error" ->
                        "Upgrade to NoisyCamp Premium to create your own booking events.")
            }
        }
    }

    private def withStudioTransaction[P](studioId: Studio#Id)
        (f: Studio => DBIOAction[Result, NoStream, Effect.All])
        (implicit request: SecuredRequest[DefaultEnv, P]): Future[Result] = {

        val user = request.identity.user

        db.run({
            daos.studio.query.
                filter(_.id === studioId).
                result.headOption.
                flatMap {
                    case Some(studio) if studio.isOwner(user) => f(studio)
                    case Some(_) => DBIO.successful(
                        Forbidden("Cannot access other customers' bookings."))
                    case None => DBIO.successful(NotFound("Booking not found."))
                }
        }.transactionally)
    }

    /** Executes the provided action after having fetched the studio and its associated bookings.
     *
     * Fetches the customer User instances in case of `StudioCustomerBooking`s.
     */
    private def withStudioBookings[T](
        id: Studio#Id, onlyActive: Boolean = false)(
        f: (Studio, Seq[(StudioBooking, Option[User])]) => Result)
        (implicit request: SecuredRequest[DefaultEnv, T]): Future[Result] = {

        val user = request.identity.user

        db.run({
            val dbStudio = daos.studio.query.
                filter(_.id === id).
                result.headOption

            dbStudio.flatMap {
                case Some(studio) => {
                    val baseQuery =
                        if (onlyActive) { daos.studioBooking.activeBookings }
                        else { daos.studioBooking.bookings }

                    baseQuery.
                        filter(_._1.studioId === id).
                        sortBy(_._1.beginsAt.desc).
                        joinLeft(daos.user.query).
                            on { case ((b, cb, mb), u) => cb.map(_.customerId === u.id) }.
                        result.
                        map(_.map { case (b, u) => (toStudioBooking(b), u) }).
                        map { bookings => Some((studio, bookings)) }
                }
                case None => DBIO.successful(None)
            }
        }.transactionally).
            map {
                case Some((studio, bookings)) if studio.isOwner(user) => {
                    f(studio, bookings)
                }
                case Some(_) => Forbidden("Only the studio owner can see bookings.")
                case None => NotFound("Studio not found.")
            }
    }

    def show(studioId: Studio#Id, bookingId: StudioBooking#Id) = SecuredAction.async {
        implicit request =>

        withStudioBookingTransaction(studioId, bookingId) { case (studio, booking, customer) =>
            for {
                equips <- bookingLocalEquipments(studio, booking)
            } yield Ok(views.html.account.studios.bookings.show(
                request.identity, studio, booking, customer, equips))
        }
    }

    def accept(studioId: Studio#Id, bookingId: StudioBooking#Id) = SecuredAction.async {
        implicit request =>

        val user = request.identity.user

        updateBooking(
            studioId, bookingId,
            "This booking has been accepted.", "You can not accept this booking.") {
            case (studio, booking, customerOpt, query) =>

            if (booking.canAccept(studio)) {
                def sendEmail = {
                    (booking, customerOpt) match {
                        case (scb: StudioCustomerBooking, Some(customer)) => {
                            for {
                                pictures <- daos.studioPicture.
                                    withStudioPictureIds(studioId).
                                    result
                                equips <- bookingLocalEquipments(studio, booking)
                                _ <- DBIO.from(emailService.sendBookingAccepted(
                                    scb, customer, studio, pictures, user, equips))
                            } yield Unit
                        }
                        case _ => DBIO.successful(Unit)
                    }
                }

                Some (
                    for {
                        _ <- query.map(_.status).
                            update(StudioBookingStatus.Valid)
                        _ <- sendEmail
                    } yield Unit
                )
            } else {
                None
            }
        }
    }

    /** Rejects the booking and refunds the customer if they paid online. */
    def reject(studioId: Studio#Id, bookingId: StudioBooking#Id) = SecuredAction.async {
        implicit request =>

         updateBooking(
            studioId, bookingId,
            "This booking has been rejected.", "Can not reject this booking.") {
            case (studio, booking, customerOpt, query) =>

            if (booking.canReject) {
                def sendEmail(refundedBooking: StudioBooking) = {
                    (refundedBooking, customerOpt) match {
                        case (scb: StudioCustomerBooking, Some(customer)) => {
                            DBIO.from(emailService.sendBookingRejected(scb, customer, studio))
                        }
                        case _ => DBIO.successful(Unit)
                    }
                }

                Some (
                    for {
                        _ <- query.map(_.status).
                            update(StudioBookingStatus.Rejected)
                        refundedBooking <- refundBooking(booking)
                        _ <- sendEmail(refundedBooking)
                    } yield Unit
                )
            } else {
                None
            }
        }
    }

    /** Cancels the booking and refunds the customer if they paid online. */
    def cancel(studioId: Studio#Id, bookingId: StudioBooking#Id) = SecuredAction.async {
        implicit request =>

        updateBooking(
            studioId, bookingId,
            "This booking has been successfuly cancelled.", "Can not cancel this booking.") {
            case (studio, booking, customerOpt, query) =>

            if (booking.ownerCanCancel) {
                def sendEmail(refundedBooking: StudioBooking) = {
                    (refundedBooking, customerOpt) match {
                        case (scb: StudioCustomerBooking, Some(customer)) => {
                            for {
                                equips <- bookingLocalEquipments(studio, booking)
                                _ <- DBIO.from(emailService.sendBookingCancelledByOwner(
                                    scb, customer, studio, equips))
                            } yield Unit
                        }
                        case _ => DBIO.successful(Unit)
                    }
                }

                Some(
                    for {
                        _ <- query.map(b => (b.status, b.cancelledAt)).
                            update((StudioBookingStatus.CancelledByOwner, Some(Instant.now)))
                        refundedBooking <- refundBooking(booking)
                        _ <- sendEmail(refundedBooking)
                    } yield Unit
                )
            } else {
                None
            }
        }
    }

    private type StudioBookingTable = daos.studioBooking.StudioBookingTable
    private type StudioCustomerBookingTable = daos.studioBooking.StudioCustomerBookingTable
    private type StudioManualBookingTable = daos.studioBooking.StudioManualBookingTable

    /** Runs the provided booking updating DB action and redirect to the booking page.
     *
     * @param onSuccessMessage the flash message to show on update success.
     * @param onFailureMessage the flash message to show on update failure (i.e. if `updateAction`
     *        returns `None`).
     * @param updateAction the DB action to run. If `None`, the update fails.
     */
    private def updateBooking[T, P](
        studioId: Studio#Id, bookingId: StudioBooking#Id,
        onSuccessMessage: String, onFailureMessage: String)(
        updateAction: (
            (Studio, StudioBooking, Option[User], Query[StudioBookingTable, StudioBookingRow, Seq])
            => Option[DBIOAction[T, NoStream, Effect.All]])
        )(
        implicit request: SecuredRequest[DefaultEnv, P]): Future[Result] = {

        val redirectTo = Redirect(routes.BookingsController.show(studioId, bookingId))

        withStudioBookingTransaction(studioId, bookingId) { (studio, booking, customer) =>
            val query = daos.studioBooking.bookingQuery.filter(_.id === booking.id)

            updateAction(studio, booking, customer, query) match {
                case Some(action) => for {
                    _ <- action.map { _ => true }
                } yield redirectTo.flashing("success" -> onSuccessMessage)
                case None => DBIO.successful(redirectTo.flashing("error" -> onFailureMessage))
            }
        }
    }

    /** Tries to refund the booking if there is an online payment.
     *
     * Saves the result in the database and returns the possibly updated `StudioBooking` object.
     */
    private def refundBooking(booking: StudioBooking):
        DBIOAction[StudioBooking, NoStream, Effect.All] = {

        booking match {
            case customerBooking: StudioCustomerBooking => {
                customerBooking.payment match {
                    case payment @ StudioBookingPaymentOnline(sessionId, intentId, _)
                        if !payment.isRefunded  => {

                        for {
                            refund <- DBIO.from(paymentService.refundPaymentIntent(intentId))

                            _ <- daos.studioBooking.query.
                                filter(_._1.id === booking.id).
                                map(_._2.map(_.stripeRefundId)).
                                update(Some(Some(refund.getId)))
                        } yield customerBooking.copy(
                            payment=payment.copy(stripeRefundId = Some(refund.getId)))
                    }
                    case _ => DBIO.successful(booking)
                }
            }
            case _ => DBIO.successful(booking)
        }

    }

    /** Executes the function within the DBIO monad, or returns a 404 response. */
    private def withStudioBookingTransaction[P](studioId: Studio#Id, bookingId: StudioBooking#Id)
        (f: (Studio, StudioBooking, Option[User]) => DBIOAction[Result, NoStream, Effect.All])
        (implicit request: SecuredRequest[DefaultEnv, P]): Future[Result] = {

        val user = request.identity.user

        db.run({
            daos.studio.query.
                filter(_.id === studioId).
                join(daos.studioBooking.query).on(_.id === _._1.studioId).
                joinLeft(daos.user.query).on {
                    case ((s, b), u) => b._2.map(_.customerId === u.id)
                }.
                filter { case ((s, b), u) => b._1.id === bookingId }.
                result.headOption.
                flatMap {
                    case Some(((studio, booking), customer)) if studio.isOwner(user) => {
                        f(studio, booking, customer)
                    }
                    case Some(_) => DBIO.successful(
                        Forbidden("Cannot access other customers' bookings."))
                    case None => DBIO.successful(NotFound("Booking not found."))
                }
        }.transactionally)
    }

    private def bookingLocalEquipments(studio: Studio, booking: StudioBooking) = {
        daos.studioBookingEquipment.
            withBookingEquipment(booking.id).
            result.
            map(_.map(_.localEquipment(studio)))
    }
}
