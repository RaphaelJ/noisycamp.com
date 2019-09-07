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

package forms.components

import org.joda.time.LocalTime
import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields

object PricingPolicyForm {

  val form = Form(
    mapping(
      "price-per-hour"          -> CustomFields.money,

      "has-evening-pricing"     -> boolean,
      "evening-begins-at"       -> optional(CustomFields.jodaLocalTime),
      "evening-price-per-hour"  -> optional(CustomFields.money),

      "has-weekend-pricing"     -> boolean,
      "weekend-price-per-hour"  -> optional(CustomFields.money),
    )(Data.apply)(Data.unapply).
      verifying("Evening begin time and pricing required.", { data =>
        !data.hasEveningPricing ||
        (data.eveningBeginsAt.isDefined && data.eveningPricePerHour.isDefined)
      }).
      verifying("Weekend pricing required.", { data =>
        !data.hasEveningPricing || data.weekendPricePerHour.isDefined }))

  case class Data(
    pricePerHour:         BigDecimal,

    hasEveningPricing:    Boolean,
    eveningBeginsAt:      Option[LocalTime],
    eveningPricePerHour:  Option[BigDecimal],

    hasWeekendPricing:    Boolean,
    weekendPricePerHour:  Option[BigDecimal])
}
