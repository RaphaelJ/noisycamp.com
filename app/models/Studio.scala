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

import java.time.{ Instant, ZoneId }
import scala.math.BigDecimal.RoundingMode

import squants.market.Money

case class Studio(
  id:               Studio#Id         = 0L,
  createdAt:        Instant           = Instant.now(),

  ownerId:          User#Id,

  name:             String,
  description:      String,

  location:         Location,
  timezone:         ZoneId,
  openingSchedule:  OpeningSchedule,
  pricingPolicy:    PricingPolicy,
  bookingPolicy:    BookingPolicy) {

  type Id = Long

  def currency = location.country.currency

  def localPricingPolicy = {
    LocalPricingPolicy(
      currency(pricingPolicy.pricePerHour),
      pricingPolicy.evening.map { eveningPolicy => LocalEveningPricingPolicy(
        eveningPolicy.beginsAt,
        currency(eveningPolicy.pricePerHour)
      ) },
      pricingPolicy.weekend.map { weekendPolicy => LocalWeekendPricingPolicy(
        currency(weekendPolicy.pricePerHour)
      ) },
    )
  }
}
