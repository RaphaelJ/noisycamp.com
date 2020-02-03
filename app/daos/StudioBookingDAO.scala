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

package daos

import scala.concurrent.ExecutionContext
import java.time.{ Duration, Instant, LocalDateTime }
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile
import squants.market

import models.{
  Studio, StudioBooking, StudioBookingPayment, StudioBookingPaymentOnline,
  StudioBookingPaymentOnsite, StudioBookingStatus, User }

class StudioBookingDAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class StudioBookingTable(tag: Tag)
    extends Table[StudioBooking](tag, "studio_booking") {

    def id                      = column[StudioBooking#Id](
      "id", O.PrimaryKey, O.AutoInc)
    def createdAt               = column[Instant]("created_at")

    def studioId                = column[Studio#Id]("studio_id")
    def customerId              = column[User#Id]("customer_id")

    def status                  = column[StudioBookingStatus.Value]("status")

    def beginsAt                = column[LocalDateTime]("begins_at")(
      localDateTimeType)

    def durationTotal           = column[Duration]("duration_total")
    def durationRegular         = column[Duration]("duration_regular")
    def durationEvening         = column[Duration]("duration_evening")
    def durationWeekend         = column[Duration]("duration_weekend")

    def studioCurrency          =
      column[market.Currency]("studio_currency_code")
    def customerCurrency        =
      column[market.Currency]("customer_currency_code")

    def studioTotal             = column[BigDecimal]("studio_total")
    def customerTotal           = column[BigDecimal]("customer_total")

    def regularPricePerHour     = column[BigDecimal]("regular_price_per_hour")
    def eveningPricePerHour     =
      column[Option[BigDecimal]]("evening_price_per_hour")
    def weekendPricePerHour     =
      column[Option[BigDecimal]]("weekend_price_per_hour")

    def paymentMethod           = column[String]("payment_method")

    def stripeCheckoutSessionId =
      column[Option[String]]("stripe_checkout_session_id")
    def stripePaymentIntentId   =
      column[Option[String]]("stripe_payment_intent_id")

    private type StudioBookingTuple = (
      StudioBooking#Id, Instant,
      Studio#Id, User#Id,
      StudioBookingStatus.Value, LocalDateTime,
      Duration, Duration, Duration, Duration,
      market.Currency, market.Currency, BigDecimal, BigDecimal,
      BigDecimal, Option[BigDecimal], Option[BigDecimal],
      StudioBookingPaymentTuple)

    private type StudioBookingPaymentTuple = (
      String, Option[String], Option[String])

    private def toStudioBooking(bookingTuple: StudioBookingTuple) = {
      StudioBooking(
        bookingTuple._1, bookingTuple._2,
        bookingTuple._3, bookingTuple._4,
        bookingTuple._5, bookingTuple._6,
        bookingTuple._7, bookingTuple._8, bookingTuple._9, bookingTuple._10,
        bookingTuple._11, bookingTuple._12,
        bookingTuple._13, bookingTuple._14,
        bookingTuple._15, bookingTuple._16, bookingTuple._17,
        toStudioBookingPayment(bookingTuple._18))
    }

    private def fromStudioBooking(booking: StudioBooking) = {
      Some((
        booking.id, booking.createdAt,
        booking.studioId, booking.customerId,
        booking.status, booking.beginsAt,
        booking.durationTotal, booking.durationRegular, booking.durationEvening,
        booking.durationWeekend,
        booking.studioCurrency, booking.customerCurrency,
        booking.studioTotal, booking.customerTotal,
        booking.regularPricePerHour, booking.eveningPricePerHour,
        booking.weekendPricePerHour,
        fromStudioBookingPayment(booking.payment)))
    }

    private def toStudioBookingPayment(
      paymentTuple: StudioBookingPaymentTuple) = {

      paymentTuple match {
        case ("online", Some(sessionId), Some(paymentId)) =>
          StudioBookingPaymentOnline(sessionId, paymentId)
        case ("onsite", _, _) => StudioBookingPaymentOnsite()
        case (value, _, _) =>
          throw new Exception(s"Invalid operator value: $value")
      }
    }

    private def fromStudioBookingPayment(payment: StudioBookingPayment) = {
      payment match {
        case StudioBookingPaymentOnline(sessionId, paymentId) => (
          "online", Some(sessionId), Some(paymentId))
        case StudioBookingPaymentOnsite() => ("onsite", None, None)
      }
    }

    def * = (
      id, createdAt,
      studioId, customerId,
      status, beginsAt,
      durationTotal, durationRegular, durationEvening, durationWeekend,
      studioCurrency, customerCurrency, studioTotal,
      customerTotal,
      regularPricePerHour, eveningPricePerHour, weekendPricePerHour,
      (paymentMethod, stripeCheckoutSessionId, stripePaymentIntentId)
    ) <> (toStudioBooking, fromStudioBooking)
  }

  lazy val query = TableQuery[StudioBookingTable]

  /** Inserts a booking and returns the newly created object with its inserted
   * ID. */
  def insert(booking: StudioBooking): DBIO[StudioBooking] = {
    query returning query.map(_.id) into ((b, id) => b.copy(id=id)) += booking
  }
}
