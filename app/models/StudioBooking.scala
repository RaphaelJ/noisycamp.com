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

package models

import java.time.{ Duration, Instant, LocalDateTime }
import java.util.UUID

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
  id:                   User#Id = 0L,
  createdAt:            Instant = Instant.now(),

  studioId:             Studio#Id,
  customerId:           User#Id,

  status:               StudioBookingStatus.Value,

  beginsAt:             LocalDateTime,

  durationTotal:        Duration,
  durationRegular:      Duration,
  durationEvening:      Duration,
  durationWeekend:      Duration,

  currency:             market.Currency,

  total:                BigDecimal,

  regularPricePerHour:  BigDecimal,
  eveningPricePerHour:  Option[BigDecimal],
  weekendPricePerHour:  Option[BigDecimal],

  payment:              StudioBookingPayment) {

  type Id = Long
}
