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
import scala.language.implicitConversions
import java.time.{ Duration, Instant, LocalDate, LocalDateTime }
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile
import squants.market

import models.{
    BookingDurations, BookingRepeat, BookingRepeatFrequency, BookingRepeatCount, BookingRepeatUntil,
    BookingTimesWithRepeat, CancellationPolicy,
    Studio, StudioBooking, StudioBookingType, StudioBookingPayment, StudioBookingPaymentOnline,
    StudioBookingPaymentOnsite, StudioBookingStatus, StudioCustomerBooking, StudioManualBooking,
    User }
import views.html.helper.repeat

class StudioBookingDAO @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider)
    (implicit executionContext: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

    import profile.api._

    type StudioBookingRow = StudioBookingDAO.StudioBookingRow
    type StudioCustomerBookingRow = StudioBookingDAO.StudioCustomerBookingRow
    type StudioManualBookingRow = StudioBookingDAO.StudioManualBookingRow

    final class StudioBookingTable(tag: Tag)
        extends Table[StudioBookingRow](tag, "studio_booking") {

        def id                      = column[StudioBooking#Id]("id", O.PrimaryKey, O.AutoInc)
        def createdAt               = column[Instant]("created_at")

        def studioId                = column[Studio#Id]("studio_id")

        def status                  = column[StudioBookingStatus.Value]("status")

        def cancelledAt             = column[Option[Instant]]("cancelled_at")
        def cancellationReason      = column[Option[String]]("cancellation_reason")

        def beginsAt                = column[LocalDateTime]("begins_at")(localDateTimeType)
        def duration                = column[Duration]("duration")
        // The end of the last repeated event. Used to speedup booking overlap queries.
        def endsAt                  = column[LocalDateTime]("ends_at")(localDateTimeType)

        def repeatType              = column[Option[String]]("repeat_type")
        def repeatFrequency         = column[Option[BookingRepeatFrequency.Val]]("repeat_frequency")
        def repeatCount             = column[Option[Int]]("repeat_count")
        def repeatUntil             = column[Option[LocalDate]]("repeat_until")

        def bookingType             = column[StudioBookingType.Value]("booking_type")

        def * = (
            id, createdAt,
            studioId,
            status,
            cancelledAt, cancellationReason,
            beginsAt, duration, endsAt,
            repeatType, repeatFrequency, repeatCount, repeatUntil,
            bookingType).mapTo[StudioBookingDAO.StudioBookingRow]
    }

    lazy val bookingQuery = TableQuery[StudioBookingTable]

    final class StudioCustomerBookingTable(tag: Tag)
        extends Table[StudioCustomerBookingRow](tag, "studio_customer_booking") {

        def id                      = column[StudioBooking#Id]("id")

        def customerId              = column[User#Id]("customer_id")

        def durationRegular         = column[Duration]("duration_regular")
        def durationEvening         = column[Duration]("duration_evening")
        def durationWeekend         = column[Duration]("duration_weekend")

        def currency                = column[market.Currency]("currency")
        def total                   = column[BigDecimal]("total")

        def pricePerHour            = column[BigDecimal]("price_per_hour")
        def eveningPricePerHour     = column[Option[BigDecimal]]("evening_price_per_hour")
        def weekendPricePerHour     = column[Option[BigDecimal]]("weekend_price_per_hour")

        def canCancel               = column[Boolean]("can_cancel")
        def cancellationNotice      = column[Option[Duration]]("cancellation_notice")

        def transactionFeeRate      = column[Option[BigDecimal]]("transaction_fee_rate")

        def paymentMethod           = column[String]("payment_method")

        def stripeCheckoutSessionId = column[Option[String]]("stripe_checkout_session_id")
        def stripePaymentIntentId   = column[Option[String]]("stripe_payment_intent_id")
        def stripeRefundId          = column[Option[String]]("stripe_refund_id")

        def * = (
            id,
            customerId,
            durationRegular, durationEvening, durationWeekend,
            currency, total,
            pricePerHour, eveningPricePerHour, weekendPricePerHour,
            canCancel, cancellationNotice,
            transactionFeeRate,
            paymentMethod, stripeCheckoutSessionId, stripePaymentIntentId, stripeRefundId).
            mapTo[StudioBookingDAO.StudioCustomerBookingRow]
    }

    lazy val customerBookingQuery = TableQuery[StudioCustomerBookingTable]

    final class StudioManualBookingTable(tag: Tag)
        extends Table[StudioManualBookingRow](tag, "studio_manual_booking") {

        def id                      = column[StudioBooking#Id]("id")
        def title                   = column[String]("title")
        def customerEmail           = column[Option[String]]("customer_email")

        def * = (id, title, customerEmail).mapTo[StudioBookingDAO.StudioManualBookingRow]
    }

    lazy val manualBookingQuery = TableQuery[StudioManualBookingTable]

    lazy val query = bookingQuery.
        joinLeft(customerBookingQuery).on(_.id === _.id).
        joinLeft(manualBookingQuery).on(_._1.id === _.id).
        map { case ((b, cb), mb) => (b, cb, mb) }

    def insert(booking: StudioCustomerBooking): DBIO[StudioCustomerBooking] = {
        val (bookingRow, customerBookingRow) =
            StudioBookingDAO.fromStudioCustomerBooking(booking)

        for {
            id <- insert(bookingRow)
            _ <- customerBookingQuery += customerBookingRow.copy(id = id)
        } yield booking.copy(id = id)
    }

    def insert(booking: StudioManualBooking): DBIO[StudioManualBooking] = {
        val (bookingRow, manualBookingRow) =
            StudioBookingDAO.fromStudioManualBooking(booking)

        for {
            id <- insert(bookingRow)
            _ <- manualBookingQuery += manualBookingRow.copy(id = id)
        } yield booking.copy(id = id)
    }

    private def insert(bookingRow: StudioBookingRow): DBIO[StudioBooking#Id] = {
        bookingQuery returning bookingQuery.map(_.id) += bookingRow
    }

    /** Creates a query with all submitted bookings (i.e. with a valid payment) */
    def bookings = {
        query.
            filter(!_._1.status.inSet(Seq(
                StudioBookingStatus.PaymentProcessing,
                StudioBookingStatus.PaymentFailure)))
    }

    /** Creates a query with all the active (i.e. not cancelled) bookings */
    def activeBookings = {
        query.
            filter(_._1.status.inSet(Seq(
                StudioBookingStatus.PendingValidation,
                StudioBookingStatus.Valid)))
    }

    /** Return true if there is already a booking that overlaps with the given booking times. */
    def hasOverlap(studio: Studio, times: BookingTimesWithRepeat): DBIO[Boolean] = {
        // Forces the use of the `localDateTimeType` mapper instead of Slick's default.
        val beginsAt = LiteralColumn(times.beginsAt)(localDateTimeType)
        val endsAt = LiteralColumn(times.endsAt)(localDateTimeType)

        activeBookings.
            filter(_._1.studioId === studio.id).
            filter(_._1.endsAt > beginsAt).
            filter(_._1.beginsAt < endsAt).
            result.
            map { bookings => bookings.exists(_.times.hasOverlap(times)) }
    }
}

object StudioBookingDAO {

    case class StudioBookingRow(
        val id:                     StudioBooking#Id,
        val createdAt:              Instant,
        val studioId:               Studio#Id,
        val status:                 StudioBookingStatus.Value,
        val cancelledAt:            Option[Instant],
        val cancellationReason:     Option[String],
        val beginsAt:               LocalDateTime,
        val duration:               Duration,
        val endsAt:                 LocalDateTime,
        val repeatType:             Option[String],
        val repeatFrequency:        Option[BookingRepeatFrequency.Val],
        val repeatCount:            Option[Int],
        val repeatUntil:            Option[LocalDate],
        val bookingType:            StudioBookingType.Value)

    case class StudioCustomerBookingRow(
        val id:                         StudioBooking#Id,
        val customerId:                 User#Id,
        val durationRegular:            Duration,
        val durationEvening:            Duration,
        val durationWeekend:            Duration,
        val currency:                   market.Currency,
        val total:                      BigDecimal,
        val pricePerHour:               BigDecimal,
        val eveningPricePerHour:        Option[BigDecimal],
        val weekendPricePerHour:        Option[BigDecimal],
        val canCancel:                  Boolean,
        val cancellationNotice:         Option[Duration],
        val transactionFeeRate:         Option[BigDecimal],
        val paymentMethod:              String,
        val stripeCheckoutSessionId:    Option[String],
        val stripePaymentIntentId:      Option[String],
        val stripeRefundId:             Option[String]) {

        require(canCancel == cancellationNotice.isDefined)
    }

    case class StudioManualBookingRow(
        val id:                         StudioBooking#Id,
        val title:                      String,
        val customerEmail:              Option[String])

    /** Implicit conversion helper that converts a set of booking rows to a polymorphic
     * StudioBooking instance. */
    implicit def toStudioBooking(
        row: (StudioBookingRow, Option[StudioCustomerBookingRow], Option[StudioManualBookingRow])):
        StudioBooking = {

        row match {
            case (bookingRow, Some(customerBookingRow), None) =>
                toStudioCustomerBooking(bookingRow, customerBookingRow)
            case (bookingRow, None, Some(manualBookingRow)) =>
                toStudioManualBooking(bookingRow, manualBookingRow)
            case _ => throw new Exception("Invalid StudioBooking DAO conversion.")
        }
    }

    implicit  def toStudioBookings(
        rows: Seq[(StudioBookingRow,
            Option[StudioCustomerBookingRow], Option[StudioManualBookingRow])]):
        Seq[StudioBooking] = rows.map(toStudioBooking _)

    private def toBookingTimes(bookingRow: StudioBookingDAO.StudioBookingRow):
        BookingTimesWithRepeat = {

        val repeat = toBookingRepeat(bookingRow)
        val times = BookingTimesWithRepeat(bookingRow.beginsAt, bookingRow.duration, repeat)
        assert(times.endsAt == bookingRow.endsAt)
        times
    }

    private def toBookingRepeat(bookingRow: StudioBookingDAO.StudioBookingRow):
        Option[BookingRepeat] = {

        val frequency = bookingRow.repeatFrequency.map { BookingRepeatFrequency.values }

        assert(bookingRow.repeatFrequency.isDefined == bookingRow.repeatType.isDefined)

        bookingRow.repeatType.map {
            case "repeat-count" => {
                assert(bookingRow.repeatCount.isDefined)
                assert(bookingRow.repeatUntil.isEmpty)
                BookingRepeatCount(bookingRow.repeatFrequency.get, bookingRow.repeatCount.get)
            }
            case "repeat-until" => {
                assert(bookingRow.repeatCount.isEmpty)
                assert(bookingRow.repeatUntil.isDefined)
                BookingRepeatUntil(bookingRow.repeatFrequency.get, bookingRow.repeatUntil.get)
            }
            case repeatType => throw new Exception(s"Invalid repeat type value: $repeatType.")
        }
    }

    private def fromBookingRepeat(repeat: Option[BookingRepeat]):
        (Option[String], Option[BookingRepeatFrequency.Val], Option[Int], Option[LocalDate]) = {

        repeat match {
            case Some(BookingRepeatCount(frequency, count)) =>
                (Some("repeat-count"), Some(frequency), Some(count), None)
            case Some(BookingRepeatUntil(frequency, until)) =>
                (Some("repeat-count"), Some(frequency), None, Some(until))
            case None => (None, None, None, None)
        }
    }

    private def toStudioCustomerBooking(
        bookingRow: StudioBookingRow, customerBookingRow: StudioCustomerBookingRow) = {

        require(bookingRow.bookingType == StudioBookingType.Customer)
        require(bookingRow.id == customerBookingRow.id)

        val durations = BookingDurations(
            customerBookingRow.durationRegular, customerBookingRow.durationEvening,
            customerBookingRow.durationWeekend)

        val payment =
            customerBookingRow.paymentMethod match {
                case "online" => {
                    assert(customerBookingRow.stripeCheckoutSessionId.isDefined)
                    assert(customerBookingRow.stripePaymentIntentId.isDefined)

                    StudioBookingPaymentOnline(
                        customerBookingRow.stripeCheckoutSessionId.get,
                        customerBookingRow.stripePaymentIntentId.get,
                        customerBookingRow.stripeRefundId)
                }
                case "onsite" => StudioBookingPaymentOnsite()
                case value => throw new Exception(s"Invalid operator value: $value")
            }

        StudioCustomerBooking(
            bookingRow.id, bookingRow.createdAt,
            bookingRow.studioId, customerBookingRow.customerId,
            bookingRow.status,
            customerBookingRow.cancellationNotice.map(CancellationPolicy(_)),
            bookingRow.cancelledAt, bookingRow.cancellationReason,
            toBookingTimes(bookingRow), durations,
            customerBookingRow.currency, customerBookingRow.total,
            customerBookingRow.pricePerHour, customerBookingRow.eveningPricePerHour,
            customerBookingRow.weekendPricePerHour,
            customerBookingRow.transactionFeeRate, payment)
    }

    private def fromStudioCustomerBooking(booking: StudioCustomerBooking)
        : (StudioBookingRow,  StudioCustomerBookingRow) = {

        val bookingType = StudioBookingType.Customer

        val (paymentMethod, sessionId, paymentId, refundId) =
            booking.payment match {
                case StudioBookingPaymentOnline(sessionId, paymentId, refundId) => (
                    "online", Some(sessionId), Some(paymentId), refundId)
                case StudioBookingPaymentOnsite() => ("onsite", None, None, None)
            }
        val (repeatType, repeatFrequency, repeatCount, repeatUntil) =
            fromBookingRepeat(booking.times.repeat)

        val bookingRow = StudioBookingRow(
            booking.id, booking.createdAt,
            booking.studioId,
            booking.status,
            booking.cancelledAt, booking.cancellationReason,
            booking.times.beginsAt, booking.times.duration, booking.times.endsAt,
            repeatType, repeatFrequency, repeatCount, repeatUntil,
            bookingType)
        val customerBookingRow = StudioCustomerBookingRow(
            booking.id,
            booking.customerId,
            booking.durations.regular, booking.durations.evening, booking.durations.weekend,
            booking.currency, booking.total,
            booking.pricePerHour, booking.eveningPricePerHour, booking.weekendPricePerHour,
            booking.cancellationPolicy.isDefined, booking.cancellationPolicy.map(_.notice),
            booking.transactionFeeRate,
            paymentMethod, sessionId, paymentId, refundId)

        (bookingRow, customerBookingRow)
    }

    private def toStudioManualBooking(
        bookingRow: StudioBookingRow, manualBookingRow: StudioManualBookingRow) = {

        require(bookingRow.bookingType == StudioBookingType.Manual)
        require(bookingRow.id == manualBookingRow.id)

        StudioManualBooking(
            bookingRow.id, bookingRow.createdAt,
            bookingRow.studioId,
            manualBookingRow.title,
            manualBookingRow.customerEmail,
            bookingRow.status,
            bookingRow.cancelledAt, bookingRow.cancellationReason,
            toBookingTimes(bookingRow))
    }

    private def fromStudioManualBooking(booking: StudioManualBooking)
        : (StudioBookingRow,  StudioManualBookingRow) = {

        val bookingType = StudioBookingType.Manual

        val (repeatType, repeatFrequency, repeatCount, repeatUntil) =
            fromBookingRepeat(booking.times.repeat)

        val bookingRow = StudioBookingRow(
            booking.id, booking.createdAt,
            booking.studioId,
            booking.status,
            booking.cancelledAt, booking.cancellationReason,
            booking.times.beginsAt, booking.times.duration, booking.times.endsAt,
            repeatType, repeatFrequency, repeatCount, repeatUntil,
            bookingType)
        val manualBookingRow = StudioManualBookingRow(
            booking.id, booking.title, booking.customerEmail)

        (bookingRow, manualBookingRow)
    }
}
