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

import squants.market.{ Currency, CurrencyExchangeRate, Money }

case class PricingPolicy(
    pricePerHour:         BigDecimal,
    evening:              Option[EveningPricingPolicy],
    weekend:              Option[WeekendPricingPolicy])

case class EveningPricingPolicy(
    beginsAt:             LocalTime,
    pricePerHour:         BigDecimal)

case class WeekendPricingPolicy(
    pricePerHour:         BigDecimal)

/** Same as `PricingPolicy` but with `Money` objects. */
case class LocalPricingPolicy(
    pricePerHour:         Money,
    evening:              Option[LocalEveningPricingPolicy],
    weekend:              Option[LocalWeekendPricingPolicy]) {

    /** If the prices of the studio varies depending on the time and day, return a [min, max] range.
     * Otherwise, returns the regular price. */
    def priceRange: Either[Money, (Money, Money)] = {
        (evening, weekend) match {
            case (Some(eveningVal), Some(weekendVal)) => {
                val min = pricePerHour min eveningVal.pricePerHour min weekendVal.pricePerHour
                val max = pricePerHour max eveningVal.pricePerHour max weekendVal.pricePerHour
                Right((min, max))
            }
            case (Some(eveningVal), None) =>  {
                val min = pricePerHour min eveningVal.pricePerHour
                val max = pricePerHour max eveningVal.pricePerHour
                Right((min, max))
            }
            case (None, Some(weekendVal)) => {
                val min = pricePerHour min weekendVal.pricePerHour
                val max = pricePerHour max weekendVal.pricePerHour
                Right((min, max))
            }
            case (None, None) => Left(pricePerHour)
        }
    }
}

object LocalPricingPolicy {
    def apply(currency: Currency, pricingPolicy: PricingPolicy): LocalPricingPolicy = {
        LocalPricingPolicy(
            currency(pricingPolicy.pricePerHour),
            pricingPolicy.evening.map { eveningPolicy => LocalEveningPricingPolicy(
                eveningPolicy.beginsAt,
                currency(eveningPolicy.pricePerHour))
            },
            pricingPolicy.weekend.map { weekendPolicy => LocalWeekendPricingPolicy(
                currency(weekendPolicy.pricePerHour))
            })
    }
}

case class LocalEveningPricingPolicy(
    beginsAt:             LocalTime,
    pricePerHour:         Money)

case class LocalWeekendPricingPolicy(
    pricePerHour:         Money)
