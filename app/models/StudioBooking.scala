/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019-2020 Raphael Javaux <raphaeljavaux@gmail.com>
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

package models

import java.time.{ Duration, Instant, LocalDate, LocalDateTime }
import java.util.{ Formatter, UUID }
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import play.api.Configuration
import squants.market

object StudioBookingStatus extends Enumeration {
    // The request for the booking has been received, but the payment has not been validated yet.
    val PaymentProcessing = Value

    // An error occured during the processing of the payment, the customer cancelled the
    // processing of the payment, or another overlapping booking occured during the processing of
    // the payment.
    val PaymentFailure = Value

    // The booking is valid and the payment has been processed (in the case of an online payment),
    // but we are still waiting for the studio owner to confirm the times.
    val PendingValidation = Value

    // Payment and owner validation succeeded.
    val Valid = Value

    // Owner rejected the booking.
    val Rejected = Value

    val CancelledByCustomer = Value
    val CancelledByOwner = Value
}

object StudioBookingType extends Enumeration {
    // A booking made through NoisyCamp's customer interface.
    val Customer = Value

    // A booking made through NoisyCamp's customer interface.
    val Manual = Value
}

sealed trait StudioBookingPayment {
    def isRefunded: Boolean
}

final case class StudioBookingPaymentOnline(
    stripeCheckoutSessionId:    String,
    stripePaymentIntentId:      String,
    stripeRefundId:             Option[String] = None
    ) extends StudioBookingPayment {

    def isRefunded: Boolean = stripeRefundId.isDefined
}

final case class StudioBookingPaymentOnsite() extends StudioBookingPayment {
    def isRefunded: Boolean = false
}

sealed trait StudioBooking {
    type Id = Long

    def id:                 StudioBooking#Id
    def createdAt:          Instant

    def studioId:           Studio#Id

    def status:             StudioBookingStatus.Value

    def cancelledAt:        Option[Instant]
    def cancellationReason: Option[String]

    def times:              BookingTimesWithRepeat

    require(cancelledAt.isDefined == isCancelled)
    require(isCancelled || cancellationReason.isEmpty)

    /** True if the booking has been accepted by the studio owner. The booking might have been later
     * cancelled. */
    def isAccepted: Boolean = {
        assert(status != StudioBookingStatus.PaymentProcessing)
        status != StudioBookingStatus.PendingValidation && status != StudioBookingStatus.Rejected
    }

    /** An active booking is a booking that is supposed to happen and for which we can't book the
     * booking perdiod against. */
    def isActive: Boolean = {
        status == StudioBookingStatus.PendingValidation ||
        status == StudioBookingStatus.Valid
    }

    def isCancelled: Boolean = {
        status == StudioBookingStatus.CancelledByCustomer ||
        status == StudioBookingStatus.CancelledByOwner
    }

    /** Returns true if the booking will start in the future, based on the studio's current time. */
    def isUpcoming(studio: Studio, now: Instant = Instant.now): Boolean = {
        times.beginsAt.isAfter(studio.currentDateTime(now))
    }

    /** Returns true if the booking already started (and might be completed), based on the studio's
     * current time. */
    def isStarted(studio: Studio, now: Instant = Instant.now): Boolean = {
        !isUpcoming(studio, now)
    }

    /** Returns true if the booking is currently going, based on the studio's current time. */
    def isOngoing(studio: Studio, now: Instant = Instant.now): Boolean = {
        val currentTime = studio.currentDateTime(now)
        !times.beginsAt.isAfter(currentTime) && currentTime.isBefore(times.endsAt)
    }

    /** Returns true of the booking has already ended, based on the studio's current time. */
    def isCompleted(studio: Studio, now: Instant = Instant.now): Boolean = {
        !studio.currentDateTime(now).isBefore(times.endsAt)
    }

    def canAccept(studio: Studio, now: Instant = Instant.now): Boolean = {
        status == StudioBookingStatus.PendingValidation && !isStarted(studio)
    }

    def canReject: Boolean = status == StudioBookingStatus.PendingValidation

    def ownerCanCancel: Boolean = status == StudioBookingStatus.Valid

    def toEvent: Event
}

/** An online booking made by a customer. */
final case class StudioCustomerBooking(
    val id:                     StudioBooking#Id = 0L,
    val createdAt:              Instant = Instant.now(),

    val studioId:               Studio#Id,
    val customerId:             User#Id,

    val status:                 StudioBookingStatus.Value,

    // If set, refunds the customer of the booking is cancelled within the specified notice.
    val cancellationPolicy:     Option[CancellationPolicy],

    val cancelledAt:            Option[Instant] = None,
    val cancellationReason:     Option[String] = None,

    val times:                  BookingTimesWithRepeat,
    val durations:              BookingDurations,

    val currency:               market.Currency,
    val total:                  BigDecimal,

    val pricePerHour:           BigDecimal,
    val eveningPricePerHour:    Option[BigDecimal],
    val weekendPricePerHour:    Option[BigDecimal],

     // The transaction fee rate collected by NoisyCamp
    val transactionFeeRate:     Option[BigDecimal],
    val payment:                StudioBookingPayment)
    extends StudioBooking {

    require(times.duration == durations.total)

    def isCustomer(user: User): Boolean = customerId == user.id

    def customerCanCancel(studio: Studio, now: Instant = Instant.now): Boolean = {
        isActive && !isStarted(studio, now)
    }

    def isPaidOnline = payment match {
        case StudioBookingPaymentOnline(_, _, _) => true
        case _ => false
    }

    /** The last (inclusive) date and time at which the online payment will be refunded if cancelled
     * by the customer.
     *
     * Returns `None` if the studio does not refund cancelled bookings.
     */
    def maxRefundDate: Option[LocalDateTime] = {
        cancellationPolicy.map { case CancellationPolicy(notice) => times.beginsAt minus notice }
    }

    def priceBreakdown(equipments: Seq[LocalEquipment]): PriceBreakdown = {
        PriceBreakdown(
            durations, currency(pricePerHour),
            eveningPricePerHour.map(currency(_)), weekendPricePerHour.map(currency(_)),
            equipments.map(_.price).collect { case Some(value) => value },
            transactionFeeRate)
    }

    /** Pseudo-random 6-characters reservation code generated from the booking ID. */
    def reservationCode(implicit config: Configuration): String = {
        val ALGO = "HmacSHA256";
        val CODE_LEN = 6

        val key = config.get[String]("play.crypto.secret").getBytes

        val mac = Mac.getInstance(ALGO)
        mac.init(new SecretKeySpec(key, ALGO))

        val codeBytes = mac.doFinal(id.toString.getBytes)

        StudioCustomerBooking.toHexString(codeBytes).take(CODE_LEN).toUpperCase
    }

    def toEvent(customer: Option[User]): Event = {
        val href = Some(controllers.account.studios.routes.BookingsController.show(studioId, id))
        Event(times.beginsAt, times.duration, customer.map(_.displayName), href)
    }

    def toEvent = toEvent(None)
}

object StudioCustomerBooking {

    /** Constructs a customer booking object from user selected booking times. */
    def apply(
        studio: Studio, customer: User, status: StudioBookingStatus.Value,
        cancellationPolicy: Option[CancellationPolicy], times: BookingTimesWithRepeat,
        equipments: Seq[LocalEquipment], transactionFeeRate: Option[BigDecimal],
        payment: StudioBookingPayment): StudioCustomerBooking = {

        val priceBreakdown = PriceBreakdown(studio, times, equipments, transactionFeeRate)

        StudioCustomerBooking(
            studio, customer, status, cancellationPolicy, times, priceBreakdown, payment)
    }

    /** Constructs a booking object from user selected booking times and a previously computed
     * price breakdown. */
    def apply(
        studio: Studio, customer: User, status: StudioBookingStatus.Value,
        cancellationPolicy: Option[CancellationPolicy], times: BookingTimesWithRepeat,
        priceBreakdown: PriceBreakdown, payment: StudioBookingPayment): StudioCustomerBooking = {

        val pricingPolicy = studio.pricingPolicy
        val localPricingPolicy = studio.localPricingPolicy

        StudioCustomerBooking(
            studioId = studio.id,
            customerId = customer.id,
            status = status,
            cancellationPolicy = cancellationPolicy,
            times = times,
            durations = priceBreakdown.durations,
            currency = studio.currency,
            total = priceBreakdown.total.amount,
            pricePerHour = pricingPolicy.pricePerHour,
            eveningPricePerHour = pricingPolicy.evening.map(_.pricePerHour),
            weekendPricePerHour = pricingPolicy.weekend.map(_.pricePerHour),
            transactionFeeRate = priceBreakdown.transactionFeeRate,
            payment = payment)
    }

    def toHexString(value: Seq[Byte]) = {
        val formatter = new Formatter()
        for (b <- value) {
            formatter.format("%02x", b.asInstanceOf[Object])
        }
        formatter.toString
    }
}

/** An online booking made by the studio's owner. */
final case class StudioManualBooking(
    val id:                     StudioBooking#Id = 0L,
    val createdAt:              Instant = Instant.now(),

    val studioId:               Studio#Id,

    val title:                  String,

    val customerEmail:          Option[String],

    val status:                 StudioBookingStatus.Value,

    val cancelledAt:            Option[Instant] = None,
    val cancellationReason:     Option[String] = None,

    val times:                  BookingTimesWithRepeat
    ) extends StudioBooking {

    def toEvent = Event(times.beginsAt, times.duration)
}

object StudioManualBooking {
    def apply(
        studio: Studio, title: String, customerEmail: Option[String],
        status: StudioBookingStatus.Value, times: BookingTimesWithRepeat):
        StudioManualBooking = {

        StudioManualBooking(
            studioId = studio.id,
            title = title,
            customerEmail = customerEmail,
            status = status,
            times = times)
    }
}
