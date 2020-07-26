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
  val Processing = Value
  val Succeeded = Value
  val Failed = Value
  val Cancelled = Value
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

    beginsAt:               LocalDateTime,

    durationTotal:          Duration,
    durationRegular:        Duration,
    durationEvening:        Duration,
    durationWeekend:        Duration,

    currency:               market.Currency,

    total:                  BigDecimal,

    regularPricePerHour:    BigDecimal,
    eveningPricePerHour:    Option[BigDecimal],
    weekendPricePerHour:    Option[BigDecimal],

    transactionFee:         Option[BigDecimal], // The fee collected by NoisyCamp.

    payment:                StudioBookingPayment) {

    type Id = Long

    def isCustomer(user: User): Boolean = customerId == user.id

    def priceBreakdown: PriceBreakdown = {
        PriceBreakdown(
            durationTotal, durationRegular, durationEvening, durationWeekend,
            currency(total), currency(regularPricePerHour),
            eveningPricePerHour.map { currency(_) }, weekendPricePerHour.map { currency (_) },
            transactionFee.map { currency(_) })
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
        Event(beginsAt, durationTotal, Some(customer.displayName), None, Seq("booking"))
    }
}

object StudioBooking {

    /** Constructs a booking object from user selected booking times. */
    def apply(
        studio: Studio, customer: User, status: StudioBookingStatus.Value,
        bookingTimes: BookingTimes, bookingBreakdown: BookingBreakdown,
        transactionFee: Option[BigDecimal], payment: StudioBookingPayment)
        : StudioBooking = {

        def durationAsHours(duration: Duration): BigDecimal = {
            BigDecimal(duration.getSeconds) / BigDecimal(3600.0)
        }

        val pricingPolicy = studio.pricingPolicy
        val localPricingPolicy = studio.localPricingPolicy

        val totalRegular =  localPricingPolicy.pricePerHour *
            durationAsHours(bookingBreakdown.durationRegular)
        val totalEvening =  localPricingPolicy.evening.map(
            _.pricePerHour * durationAsHours(bookingBreakdown.durationEvening))
        val totalWeekend = localPricingPolicy.weekend.map(
            _.pricePerHour * durationAsHours(bookingBreakdown.durationWeekend))

        val currency = totalRegular.currency
        val zero = currency(0)
        val total = totalRegular + totalEvening.getOrElse(zero) + totalWeekend.getOrElse(zero)

        StudioBooking(
            studioId = studio.id,
            customerId = customer.id,
            status = status,
            beginsAt = bookingTimes.beginsAt,
            durationTotal = bookingBreakdown.durationTotal,
            durationRegular = bookingBreakdown.durationRegular,
            durationEvening = bookingBreakdown.durationEvening,
            durationWeekend = bookingBreakdown.durationWeekend,
            currency = currency,
            total = total.amount,
            regularPricePerHour = pricingPolicy.pricePerHour,
            eveningPricePerHour = pricingPolicy.evening.map(_.pricePerHour),
            weekendPricePerHour = pricingPolicy.weekend.map(_.pricePerHour),
            transactionFee = transactionFee,
            payment = payment)
    }
}
