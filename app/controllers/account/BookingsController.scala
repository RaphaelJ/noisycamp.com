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

package controllers.account

import javax.inject._
import java.time.Instant

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api._
import play.api.mvc._

import auth.DefaultEnv
import daos.CustomColumnTypes
import daos.StudioBookingDAO.toStudioBooking
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import models.{
    FacebookEvent, FacebookEventName, FacebookContentCategory, FacebookContentType,
    FacebookContentTypeValues, FacebookContentIds, FacebookValue, FacebookCurrency,
    LocalEquipment, Studio, StudioBooking, StudioCustomerBooking, StudioBookingType,
    StudioBookingStatus, StudioBookingPaymentOnline, StudioBookingPaymentOnsite, User }

@Singleton
class BookingsController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc)
    with CustomColumnTypes  {

    import profile.api._

    /** Lists the bookings of the current user. */
    def index = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run({
            daos.studioBooking.bookings.
                filter(_._1.bookingType === StudioBookingType.Customer).
                filter(_._2.map { _.customerId === user.id }).
                join(daos.studio.query).on(_._1.studioId === _.id).
                result
        }).map { bookings =>
            Ok(views.html.account.bookings.index(
                request.identity,
                bookings.map { case (booking, studio) => (toStudioBooking(booking), studio) }))
        }
    }

    def show(id: StudioBooking#Id) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        withStudioCustomerBookingTransaction(id) { (studio, booking, owner, equips) =>
            val fbEvent = (request.flash.get("redirect-from"), booking) match {
                case (Some("payment-success"), scb: StudioCustomerBooking) => {
                    val transactionFee = scb.transactionFee.getOrElse(studio.currency(0))

                    Seq(FacebookEvent(
                        FacebookEventName.Purchase,
                        Seq(
                            FacebookContentCategory("studio"),
                            FacebookContentType(FacebookContentTypeValues.Product),
                            FacebookContentIds(Seq(studio.id.toString)),
                            FacebookValue(transactionFee.amount),
                            FacebookCurrency(transactionFee.currency))))
                }
                case _ => Seq.empty
            }

            DBIO.successful(Ok(views.html.account.bookings.show(
                request.identity, studio, owner, booking, equips, facebookEvents = fbEvent)))
        }
    }

    def cancel(id: StudioBooking#Id) = SecuredAction.async { implicit request =>
        val user = request.identity.user
        val now = Instant.now

        val redirectTo = Redirect(routes.BookingsController.show(id))

        withStudioCustomerBookingTransaction(id) { (studio, booking, owner, equips) =>
            if (booking.customerCanCancel(studio, now)) {
                for {
                    _ <- daos.studioBooking.bookingQuery.
                        filter(_.id === booking.id).
                        map { b => (b.status, b.cancelledAt) }.
                        update((StudioBookingStatus.CancelledByCustomer, Some(now)))
                    refundedBooking <- refundBooking(studio, booking, now)
                    _ <- DBIO.from(emailService.sendBookingCancelledByCustomer(
                        refundedBooking, user, studio, owner, equips))
                } yield redirectTo.
                    flashing("success" -> "This booking has been successfuly cancelled.")
            } else {
                DBIO.successful(redirectTo.
                    flashing("error" -> "Can not cancel this booking."))
            }
        }
    }

    /** Executes the function within the DBIO monad, or returns a 404 response. */
    private def withStudioCustomerBookingTransaction[P](bookingId: StudioBooking#Id)
        (f: ((Studio, StudioCustomerBooking, User, Seq[LocalEquipment])
            => DBIOAction[Result, NoStream, Effect.All]))
        (implicit request: SecuredRequest[DefaultEnv, P]): Future[Result] = {

        val user = request.identity.user

        db.run({
            daos.studioBooking.query.
                filter(_._1.id === bookingId).
                filter(_._1.bookingType === StudioBookingType.Customer).
                filter(_._2.map(_.customerId === user.id)).
                join(daos.studio.query).on(_._1.studioId === _.id).
                join(daos.user.query).on(_._2.ownerId === _.id).
                result.headOption.
                map(_.map { case ((bookingRow, studio), owner) =>
                    ((toStudioBooking(bookingRow), studio), owner)
                }).
                flatMap {
                    case Some(((booking: StudioCustomerBooking, studio), owner)) => {
                        daos.studioBookingEquipment.
                            withBookingEquipment(booking.id).
                            result.
                            flatMap { equips =>
                                f(studio, booking, owner, equips.map(_.localEquipment(studio)))
                            }
                        }
                    case _ => DBIO.successful(NotFound("Booking not found."))
                }: DBIOAction[Result, NoStream, Effect.All]

        }.transactionally)
    }

    /** Tries to refund the booking if there is an online payment and if the customer cancels the
     * booking within the cancellation policy.
     *
     * Saves the result in the database and returns the possibly updated `StudioBooking` object.
     */
    private def refundBooking(studio: Studio, booking: StudioCustomerBooking, now: Instant):
        DBIOAction[StudioCustomerBooking, NoStream, Effect.All] = {

        lazy val shouldBeRefunded =
            booking.maxRefundDate.
                map { maxRefundDate => !studio.currentDateTime(now).isAfter(maxRefundDate) }.
                getOrElse(false)

        booking.payment match {
            case payment @ StudioBookingPaymentOnline(sessionId, intentId, _)
                if !payment.isRefunded && shouldBeRefunded => {

                for {
                    refund <- DBIO.from(paymentService.refundPaymentIntent(intentId))

                    _ <- daos.studioBooking.query.
                        filter(_._1.id === booking.id).
                        filter(_._1.bookingType === StudioBookingType.Customer).
                        map(_._2.map(_.stripeRefundId)).
                        update(Some(Some(refund.getId)))
                } yield booking.copy(payment=payment.copy(stripeRefundId = Some(refund.getId)))
            }
            case _ => DBIO.successful(booking)
        }
    }
}
