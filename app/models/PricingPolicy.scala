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

import java.time.LocalTime

import squants.market.{ Currency, Money }

import i18n.ExchangeRates

case class PricingPolicy(
  pricePerHour:         BigDecimal,
  evening:              Option[EveningPricingPolicy],
  weekend:              Option[WeekendPricingPolicy])

case class EveningPricingPolicy(
  beginsAt:             LocalTime,
  pricePerHour:         BigDecimal)

case class WeekendPricingPolicy(
  pricePerHour:         BigDecimal)

/** Same as `PricingPolicy` but with a local currency. */
case class LocalPricingPolicy(
  pricePerHour:         Money,
  evening:              Option[LocalEveningPricingPolicy],
  weekend:              Option[LocalWeekendPricingPolicy]) {

  /** Converts the prices to the given currency. */
  def in(curr: Currency)(implicit er: ExchangeRates) = {
    copy(
      pricePerHour = er.exchange(pricePerHour, curr),
      evening = evening.map(_.in(curr)(er)),
      weekend = weekend.map(_.in(curr)(er)))
  }
}

case class LocalEveningPricingPolicy(
  beginsAt:             LocalTime,
  pricePerHour:         Money) {

  def in(curr: Currency)(implicit er: ExchangeRates) = {
    copy(pricePerHour = er.exchange(pricePerHour, curr))
  }
}

case class LocalWeekendPricingPolicy(
  pricePerHour:         Money) {

  def in(curr: Currency)(implicit er: ExchangeRates) = {
    copy(pricePerHour = er.exchange(pricePerHour, curr))
  }
}
