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

import java.time.{ Duration, Instant, LocalDateTime }
import java.util.{ Formatter, UUID }
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import play.api.Configuration
import squants.market

object StudioBookingStatus extends Enumeration {
    // The request for the booking has been received, but the payment has not been
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

    val CancelledByCustomer = Value
    val CancelledByOwner = Value
}

sealed trait StudioBookingPayment

final case class StudioBookingPaymentOnline(
    stripeCheckoutSessionId:  String,
    stripePaymentIntentId:    String
    ) extends StudioBookingPayment

final case class StudioBookingPaymentOnsite() extends StudioBookingPayment

case class StudioBooking(
    id:                     StudioBooking#Id = 0L,
    createdAt:              Instant = Instant.now(),

    studioId:               Studio#Id,
    customerId:             User#Id,

    status:                 StudioBookingStatus.Value,

    // If set, allow the customer to cancel the booking within the specified notice.
    cancellationPolicy:     Option[CancellationPolicy],

    cancellationReason:     Option[String] = None,

    times:                  BookingTimes,
    durations:              BookingDurations,

    currency:               market.Currency,
    total:                  BigDecimal,

    pricePerHour:           BigDecimal,
    eveningPricePerHour:    Option[BigDecimal],
    weekendPricePerHour:    Option[BigDecimal],

    transactionFeeRate:     Option[BigDecimal], // The transaction fee rate collected by NoisyCamp
    payment:                StudioBookingPayment) {

    type Id = Long

    require(times.duration == durations.total)

    def canCancel: Boolean = cancellationPolicy.isDefined

    /** An active booking is a booking that is supposed to happen and for which we can't book the
     * booking perdiod again. */
    def isActive: Boolean = {
        status == StudioBookingStatus.PendingValidation ||
        status == StudioBookingStatus.Valid
    }

    /** Returns true of the booking has already started, based on the studio's current time. */
    def isCompleted(studio: Studio): Boolean = {
        studio.currentDateTime.isAfter(times.beginsAt)
    }

    def isCustomer(user: User): Boolean = customerId == user.id

    def priceBreakdown: PriceBreakdown = {
        PriceBreakdown(durations, currency(pricePerHour), eveningPricePerHour.map(currency(_)),
            weekendPricePerHour.map(currency(_)), transactionFeeRate)
    }

    /** Pseudo-random 6-characters reservation code generated from the booking ID. */
    def reservationCode(implicit config: Configuration): String = {
        val ALGO = "HmacSHA256";
        val CODE_LEN = 6

        val key = config.get[String]("play.crypto.secret").getBytes

        val mac = Mac.getInstance(ALGO)
        mac.init(new SecretKeySpec(key, ALGO))

        val codeBytes = mac.doFinal(id.toString.getBytes)

        toHexString(codeBytes).take(CODE_LEN).toUpperCase
    }

    private def toHexString(value: Seq[Byte]) = {
        val formatter = new Formatter()
        for (b <- value) {
            formatter.format("%02x", b.asInstanceOf[Object])
        }
        formatter.toString
    }

    def toEvent(customer: User): Event = {
        val href = Some(controllers.account.studios.routes.BookingsController.show(studioId, id))
        Event(times.beginsAt, times.duration, Some(customer.displayName), href, Seq("booking"))
    }
}

object StudioBooking {

    /** Constructs a booking object from user selected booking times. */
    def apply(
        studio: Studio, customer: User, status: StudioBookingStatus.Value,
        cancellationPolicy: Option[CancellationPolicy], times: BookingTimes,
        transactionFeeRate: Option[BigDecimal], payment: StudioBookingPayment): StudioBooking = {

        val priceBreakdown = PriceBreakdown(studio, times, transactionFeeRate)

        StudioBooking(studio, customer, status, cancellationPolicy, times, priceBreakdown, payment)
    }

    /** Constructs a booking object from user selected booking times and an previously computed
     * price breakdown. */
    def apply(
        studio: Studio, customer: User, status: StudioBookingStatus.Value,
        cancellationPolicy: Option[CancellationPolicy], times: BookingTimes,
        priceBreakdown: PriceBreakdown, payment: StudioBookingPayment): StudioBooking = {

        val pricingPolicy = studio.pricingPolicy
        val localPricingPolicy = studio.localPricingPolicy

        StudioBooking(
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
}
