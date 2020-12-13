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
import models.{ Studio, StudioBooking, StudioBookingPaymentOnline, StudioBookingPaymentOnsite,
    StudioBookingStatus, User }
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
            val bookingEvents = bookings.map { 
                case (booking, user) => booking.toEvent(Some(user), Seq("booking")) }

            Ok(views.html.account.studios.bookings.calendar(
                request.identity, studio, bookingEvents))
        }
    }

    def calendarSync(id: Studio#Id) = SecuredAction { implicit request =>
        val user = request.identity.user

        if (user.plan.calendarSync) {
            Ok("Feature not yet implemented.")
        } else {
            Redirect(_root_.controllers.account.routes.PremiumController.upgrade).
                flashing("error" -> 
                    ("Upgrade to NoisyCamp Premium to synchronize NoisyCamp with your favorite " +
                    "calendar app."))
        }
    }

    def create(id: Studio#Id) = SecuredAction { implicit request =>
        val user = request.identity.user

        if (user.plan.manualBookings) {
            Ok("Feature not yet implemented.")
        } else {
            Redirect(_root_.controllers.account.routes.PremiumController.upgrade).
                flashing("error" ->
                    "Upgrade to NoisyCamp Premium to create your own booking events.")
        }
    }

    private def withStudioBookings[T](
        id: Studio#Id, onlyActive: Boolean = false)(
        f: ((Studio, Seq[(StudioBooking, User)]) => Result))
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
                        else {daos.studioBooking.bookings }

                    baseQuery.
                        filter(_.studioId === id).
                        join(daos.user.query).on(_.customerId === _.id).
                        sortBy(_._1.beginsAt.desc).
                        result.
                        map { bookings => Some((studio, bookings)) }
                }
                case None => DBIO.successful(None)
            }
        }.transactionally).
            map {
                case Some((studio, bookings)) if studio.isOwner(user) => f(studio, bookings)
                case Some(_) => Forbidden("Only the studio owner can see bookings.")
                case None => NotFound("Studio not found.")
            }
    }

    def show(studioId: Studio#Id, bookingId: StudioBooking#Id) = SecuredAction.async {
        implicit request =>

        withStudioBookingTransaction(studioId, bookingId, { case (studio, booking, customer) =>
            DBIO.successful(Ok(views.html.account.studios.bookings.show(
                request.identity, studio, booking, customer)))
        })
    }

    def accept(studioId: Studio#Id, bookingId: StudioBooking#Id) = SecuredAction.async {
        implicit request =>

        val user = request.identity.user

        updateBooking(
            studioId, bookingId,
            "This booking has been accepted.", "You can not accept this booking.") {
            case (studio, booking, customer, query) =>

            if (booking.canAccept(studio)) {
                Some (
                    for {
                        _ <- query.map(_.status).
                            update(StudioBookingStatus.Valid)
                        pictures <- daos.studioPicture.withStudioPictureIds(studioId).result
                        _ <- DBIO.from(emailService.sendBookingAccepted(
                            booking, customer, studio, pictures, user))
                    }  yield Unit
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
            case (studio, booking, customer, query) =>

            if (booking.canReject) {
                Some (
                    for {
                        _ <- query.map(_.status).
                            update(StudioBookingStatus.Rejected)
                        refundedBooking <- refundBooking(booking)
                        _ <- DBIO.from(emailService.sendBookingRejected(
                            refundedBooking, customer, studio))
                    }  yield Unit
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
            case (studio, booking, customer, query) =>

            if (booking.ownerCanCancel) {
                Some (
                    for {
                        _ <- query.map(b => (b.status, b.cancelledAt)).
                            update((StudioBookingStatus.CancelledByOwner, Some(Instant.now)))
                        refundedBooking <- refundBooking(booking)
                        _ <- DBIO.from(emailService.sendBookingCancelledByOwner(
                            refundedBooking, customer, studio))
                    }  yield Unit
                )
            } else {
                None
            }
        }
    }

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
            (Studio, StudioBooking, User,
            Query[StudioBookingDAO#StudioBookingTable, StudioBooking, Seq]) 
            => Option[DBIOAction[T, NoStream, Effect.All]])
        )(
        implicit request: SecuredRequest[DefaultEnv, P]): Future[Result] = {
    
        val redirectTo = Redirect(routes.BookingsController.show(studioId, bookingId))

        withStudioBookingTransaction(studioId, bookingId, { 
            case (studio, booking, customer) =>
            
            val query = daos.studioBooking.query.filter(_.id === booking.id)
            
            updateAction(studio, booking, customer, query) match {
                case Some(action) => for { 
                    _ <- action.map { _ => true }
                } yield redirectTo.flashing("success" -> onSuccessMessage)
                case None => DBIO.successful(redirectTo.flashing("error" -> onFailureMessage))
            }
        })
    }

    /** Tries to refund the booking if there is an online payment.
     * 
     * Saves the result in the database and returns the updated `StudioBooking` object.
     */
    private def refundBooking(booking: StudioBooking):
        DBIOAction[StudioBooking, NoStream, Effect.All] = {

        booking.payment match {
            case payment @ StudioBookingPaymentOnline(sessionId, intentId, _) 
                if !payment.isRefunded  => {
                
                for {
                    refund <- DBIO.from(paymentService.refundPaymentIntent(intentId))

                    _ <- daos.studioBooking.query.
                        filter(_.id === booking.id).
                        map(_.stripeRefundId).
                        update(Some(refund.getId))
                } yield booking.copy(payment=payment.copy(stripeRefundId = Some(refund.getId)))
            }
            case _ => DBIO.successful(booking)
        }
    }

    /** Executes the function within the DBIO monad, or returns a 404 response. */
    private def withStudioBookingTransaction[P](studioId: Studio#Id, bookingId: StudioBooking#Id,
        f: ((Studio, StudioBooking, User) => DBIOAction[Result, NoStream, Effect.All]))
        (implicit request: SecuredRequest[DefaultEnv, P]): Future[Result] = {

        val user = request.identity.user

        db.run({
            daos.studio.query.
                join(daos.studioBooking.query).on(_.id === _.studioId).
                join(daos.user.query).on { case ((s, b), u) => b.customerId === u.id }.
                filter { case ((s, b), u) =>
                    s.id === studioId &&
                    b.id === bookingId
                }.
                result.headOption.
                flatMap {
                    case Some(((studio, booking), customer)) if studio.isOwner(user) => {
                        f(studio, booking, customer)
                    }
                    case Some(_) => DBIO.successful(
                        Forbidden("Cannot access other customers' bookings."))
                    case None => DBIO.successful(NotFound("Booking not found."))
                }: DBIOAction[Result, NoStream, Effect.All]
        }.transactionally)
    }
}
