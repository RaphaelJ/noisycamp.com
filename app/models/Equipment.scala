/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2020  Raphael Javaux <raphael@noisycamp.com>
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

import java.time.Duration

import squants.market.{ Currency, CurrencyExchangeRate, Money }

import misc.EquipmentCategory

sealed trait EquipmentPrice
final case class EquipmentPricePerSession(val value: BigDecimal) extends EquipmentPrice
final case class EquipmentPricePerHour(val value: BigDecimal) extends EquipmentPrice

/** A equipment associated with a studio. */
case class Equipment(
    id:             Equipment#Id = 0L,

    category:       Option[EquipmentCategory.Val],
    details:        Option[String],

    price:          Option[EquipmentPrice]) {

    type Id = Long

    def localEquipment(studio: Studio) = {
        val currency = studio.location.address.country.currency
        LocalEquipment(id, category, details, price.map(LocalEquipmentPrice(_, currency)))
    }
}

sealed trait LocalEquipmentPrice {
    /** Computes the cost of leasing the equipment for the duration of the session. */ 
    def sessionTotal(duration: Duration): Money
}
final case class LocalEquipmentPricePerHour(val value: Money) extends LocalEquipmentPrice {
    def sessionTotal(duration: Duration): Money = {
        val nHours = BigDecimal(duration.getSeconds) / BigDecimal(3600.0)
        value * nHours
    }
}
final case class LocalEquipmentPricePerSession(val value: Money) extends LocalEquipmentPrice {
    def sessionTotal(duration: Duration): Money = value
}

/** Same as `PricingPolicy` but with `Money` objects. */
case class LocalEquipment(
    id:             Equipment#Id = 0L,

    category:       Option[EquipmentCategory.Val],
    details:        Option[String],
    
    price:          Option[LocalEquipmentPrice])

object LocalEquipmentPrice {
    def apply(price: EquipmentPrice, currency: Currency) = {
        price match {
            case EquipmentPricePerHour(value) => LocalEquipmentPricePerHour(currency(value))
            case EquipmentPricePerSession(value) => LocalEquipmentPricePerSession(currency(value))
        }
    }
}
