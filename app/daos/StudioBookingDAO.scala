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
    BookingDurations, BookingTimes, CancellationPolicy, Studio, StudioBooking, StudioBookingPayment,
    StudioBookingPaymentOnline, StudioBookingPaymentOnsite, StudioBookingStatus, User }

class StudioBookingDAO @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider)
    (implicit executionContext: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

    import profile.api._

    final class StudioBookingTable(tag: Tag)
        extends Table[StudioBooking](tag, "studio_booking") {

        def id                      = column[StudioBooking#Id]("id", O.PrimaryKey, O.AutoInc)
        def createdAt               = column[Instant]("created_at")

        def studioId                = column[Studio#Id]("studio_id")
        def customerId              = column[User#Id]("customer_id")

        def status                  = column[StudioBookingStatus.Value]("status")

        def canCancel               = column[Boolean]("can_cancel")
        def cancellationNotice      = column[Option[Duration]]("cancellation_notice")

        def cancelledAt             = column[Option[Instant]]("cancelled_at")
        def cancellationReason      = column[Option[String]]("cancellation_reason")

        def beginsAt                = column[LocalDateTime]("begins_at")(localDateTimeType)
        def duration                = column[Duration]("duration")

        def endsAt                  = column[LocalDateTime]("ends_at")(localDateTimeType)

        def durationRegular         = column[Duration]("duration_regular")
        def durationEvening         = column[Duration]("duration_evening")
        def durationWeekend         = column[Duration]("duration_weekend")

        def currency                = column[market.Currency]("currency")
        def total                   = column[BigDecimal]("total")

        def pricePerHour            = column[BigDecimal]("price_per_hour")
        def eveningPricePerHour     = column[Option[BigDecimal]]("evening_price_per_hour")
        def weekendPricePerHour     = column[Option[BigDecimal]]("weekend_price_per_hour")

        def transactionFeeRate      = column[Option[BigDecimal]]("transaction_fee_rate")

        def paymentMethod           = column[String]("payment_method")

        def stripeCheckoutSessionId = column[Option[String]]("stripe_checkout_session_id")
        def stripePaymentIntentId   = column[Option[String]]("stripe_payment_intent_id")

        private type StudioBookingTuple = (
            StudioBooking#Id, Instant,
            Studio#Id, User#Id,
            StudioBookingStatus.Value, CancellationPolicyTuple, Option[Instant], Option[String],
            BookingTimesTuple,
            BookingDurationsTuple,
            market.Currency, BigDecimal,
            BigDecimal, Option[BigDecimal], Option[BigDecimal],
            Option[BigDecimal],
            StudioBookingPaymentTuple)

        private type CancellationPolicyTuple = (Boolean, Option[Duration])

        private type BookingTimesTuple = (LocalDateTime, Duration, LocalDateTime)

        private type BookingDurationsTuple = (Duration, Duration, Duration)

        private type StudioBookingPaymentTuple = (String, Option[String], Option[String])

        private def toStudioBooking(bookingTuple: StudioBookingTuple) = {
            StudioBooking(
                bookingTuple._1, bookingTuple._2,
                bookingTuple._3, bookingTuple._4,
                bookingTuple._5, toCancellationPolicy(bookingTuple._6), bookingTuple._7,
                bookingTuple._8,
                toBookingTimes(bookingTuple._9),
                BookingDurations.tupled(bookingTuple._10),
                bookingTuple._11, bookingTuple._12,
                bookingTuple._13, bookingTuple._14, bookingTuple._15,
                bookingTuple._16,
                toStudioBookingPayment(bookingTuple._17))
        }

        private def fromStudioBooking(booking: StudioBooking) = {
            Some((
                booking.id, booking.createdAt,
                booking.studioId, booking.customerId,
                booking.status, fromCancellationPolicy(booking.cancellationPolicy),
                booking.cancelledAt, booking.cancellationReason,
                fromBookingTimes(booking.times),
                BookingDurations.unapply(booking.durations).get,
                booking.currency, booking.total,
                booking.pricePerHour, booking.eveningPricePerHour, booking.weekendPricePerHour,
                booking.transactionFeeRate,
                fromStudioBookingPayment(booking.payment)))
        }

        private def toCancellationPolicy(tuple: CancellationPolicyTuple) = {
            require(tuple._1 == tuple._2.isDefined)
            tuple._2.map(CancellationPolicy(_))
        }

        private def fromCancellationPolicy(policy: Option[CancellationPolicy]) = {
            (policy.isDefined, policy.map(_.notice))
        }

        private def toBookingTimes(timesTuple: BookingTimesTuple) = {
            val times = BookingTimes(timesTuple._1, timesTuple._2)
            assert(times.endsAt == timesTuple._3)
            times
        }

        private def fromBookingTimes(times: BookingTimes) = {
            (times.beginsAt, times.duration, times.endsAt)
        }

        private def toStudioBookingPayment(paymentTuple: StudioBookingPaymentTuple) = {
            paymentTuple match {
                case ("online", Some(sessionId), Some(paymentId)) =>
                    StudioBookingPaymentOnline(sessionId, paymentId)
                case ("onsite", _, _) => StudioBookingPaymentOnsite()
                case (value, _, _) => throw new Exception(s"Invalid operator value: $value")
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
            status, (canCancel, cancellationNotice), cancelledAt, cancellationReason,
            (beginsAt, duration, endsAt),
            (durationRegular, durationEvening, durationWeekend),
            currency, total,
            pricePerHour, eveningPricePerHour, weekendPricePerHour,
            transactionFeeRate,
            (paymentMethod, stripeCheckoutSessionId, stripePaymentIntentId)
            ) <> (toStudioBooking, fromStudioBooking)
    }

    lazy val query = TableQuery[StudioBookingTable]

    /** Inserts a booking and returns the newly created object with its inserted
     * ID. */
    def insert(booking: StudioBooking): DBIO[StudioBooking] = {
        query returning query.map(_.id) into ((b, id) => b.copy(id=id)) += booking
    }

    /** Creates a query with all the active (i.e. not cancelled) bookings */
    def activeBookings = {
        query.
            filter(_.status.inSet(Seq(
                StudioBookingStatus.PendingValidation,
                StudioBookingStatus.Valid)))
    }

    /** Return true if there is already a booking that overlaps with the given booking times. */
    def hasOverlap(studio: Studio, times: BookingTimes): DBIO[Boolean] = {
        // Forces the use of the `localDateTimeType` mapper instead of Slick's default.
        val beginsAt = LiteralColumn(times.beginsAt)(localDateTimeType)
        val endsAt = LiteralColumn(times.endsAt)(localDateTimeType)

        activeBookings.
            filter(_.studioId === studio.id).
            filter(_.endsAt > beginsAt).
            filter(_.beginsAt < endsAt).
            exists.
            result
    }
}
