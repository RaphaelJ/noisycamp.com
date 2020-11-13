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

import java.time.ZoneId
import javax.inject._

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api._
import play.api.mvc._

import auth.DefaultEnv
import daos.CustomColumnTypes
import models.{ Studio, StudioBooking, StudioBookingPaymentOnline, StudioBookingPaymentOnsite,
    StudioBookingStatus, User }
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import misc.PaymentService

@Singleton
class BookingsController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc)
    with CustomColumnTypes {

    import profile.api._

    def index(id: Studio#Id) = silhouette.SecuredAction.async { implicit request =>
        withStudioBookings(id, { case (studio, bookings) =>
            Ok(views.html.account.studios.bookings.index(request.identity, studio, bookings))
        })
    }

    def calendar(id: Studio#Id) = silhouette.SecuredAction.async { implicit request =>
        withStudioBookings(id, { case (studio, bookings) =>
            val bookingEvents = bookings.map { 
                case (booking, user) => booking.toEvent(Some(user), Seq("booking")) }

            Ok(views.html.account.studios.bookings.calendar(
                request.identity, studio, bookingEvents))
        })
    }

    private def withStudioBookings[T](id: Studio#Id,
        f: ((Studio, Seq[(StudioBooking, User)]) => Result))
        (implicit request: SecuredRequest[DefaultEnv, T]): Future[Result] = {

        val user = request.identity.user

        db.run({
            val dbStudio = daos.studio.query.
                filter(_.id === id).
                result.headOption

            dbStudio.flatMap {
                case Some(studio) => {
                    daos.studioBooking.query.
                        filter(_.studioId === id).
                        join(daos.user.query).on(_.customerId === _.id).
                        sortBy(_._1.beginsAt.desc).
                        result.
                        map { bookings =>
                            Some((studio, bookings))
                        }
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

    def show(studioId: Studio#Id, bookingId: StudioBooking#Id) = silhouette.SecuredAction.async {
        implicit request =>

        withStudioBookingTransaction(studioId, bookingId, { case (studio, booking, customer) =>
            DBIO.successful(Ok(views.html.account.studios.bookings.show(
                request.identity, studio, booking, customer)))
        })
    }

    def accept(studioId: Studio#Id, bookingId: StudioBooking#Id) = silhouette.SecuredAction.async {
        implicit request =>

        val redirectTo = Redirect(routes.BookingsController.show(studioId, bookingId))

        withStudioBookingTransaction(studioId, bookingId, { case (studio, booking, customer) =>
            if (booking.status == StudioBookingStatus.PendingValidation && 
                !booking.isStarted(studio)) {

                daos.studioBooking.query.
                    filter(_.id === bookingId).
                    map(_.status).
                    update(StudioBookingStatus.Valid).
                    map { _ => redirectTo.flashing("success" -> "This booking has been accepted.") }
            } else {
                DBIO.successful(redirectTo.flashing("error" -> "You can not accept this booking."))
            }
        })
    }

    /** Rejects the booking and refunds the customer if they paid online. */
    def reject(studioId: Studio#Id, bookingId: StudioBooking#Id) = silhouette.SecuredAction.async {
        implicit request =>

        val redirectTo = Redirect(routes.BookingsController.show(studioId, bookingId))

        withStudioBookingTransaction(studioId, bookingId, { case (studio, booking, customer) =>
            if (booking.status == StudioBookingStatus.PendingValidation) {
                (booking.payment match {
                    case StudioBookingPaymentOnline(sessionId, intentId) => {
                        DBIO.from(paymentService.refundPayment(intentId)).map(Some(_))
                    }
                    case StudioBookingPaymentOnsite() => DBIO.successful(None)
                }).flatMap { _ =>
                    daos.studioBooking.query.
                        filter(_.id === bookingId).
                        map(_.status).
                        update(StudioBookingStatus.Rejected).
                        map { _ => 
                            redirectTo.flashing("success" -> "This booking has been rejected.") }    
                }
            } else {
                DBIO.successful(redirectTo.flashing("error" -> "Can not reject this booking."))
            }
        })
    }

    /** Cancels the booking and refunds the customer if they paid online. */
    def cancel(studioId: Studio#Id, bookingId: StudioBooking#Id) = silhouette.SecuredAction.async {
        implicit request =>

        val redirectTo = Redirect(routes.BookingsController.show(studioId, bookingId))

        withStudioBookingTransaction(studioId, bookingId, { case (studio, booking, customer) =>
            if (booking.ownerCanCancel) {
                (booking.payment match {
                    case StudioBookingPaymentOnline(sessionId, intentId) => {
                        DBIO.from(paymentService.refundPayment(intentId)).map(Some(_))
                    }
                    case StudioBookingPaymentOnsite() => DBIO.successful(None)
                }).flatMap { _ =>
                    daos.studioBooking.query.
                        filter(_.id === bookingId).
                        map(_.status).
                        update(StudioBookingStatus.CancelledByOwner).
                        map { _ => 
                            redirectTo.flashing(
                                "success" -> "This booking has been successfuly cancelled.") }    
                }
            } else {
                DBIO.successful(redirectTo.flashing("error" -> "Can not cancel this booking."))
            }
        })
    }

    /** Executes the function within the DBIO monad, or returns a 404 response. */
    private def withStudioBookingTransaction[T](studioId: Studio#Id, bookingId: StudioBooking#Id,
        f: ((Studio, StudioBooking, User) => DBIOAction[Result, NoStream, Effect.All]))
        (implicit request: SecuredRequest[DefaultEnv, T]): Future[Result] = {

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
