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
import models.{ PricingPolicy, EveningPricingPolicy, WeekendPricingPolicy }

object PricingPolicyForm {

  type Data = PricingPolicy

  val form = Form {
    type TupleType = (
      BigDecimal, Boolean, Option[LocalTime], Option[BigDecimal], Boolean,
      Option[BigDecimal])

    tuple(
      "price-per-hour"          -> CustomFields.amount,

      "has-evening-pricing"     -> boolean,
      "evening-begins-at"       -> optional(CustomFields.jodaLocalTime),
      "evening-price-per-hour"  -> optional(CustomFields.amount),

      "has-weekend-pricing"     -> boolean,
      "weekend-price-per-hour"  -> optional(CustomFields.amount),
    ).
      verifying("Evening begin time and pricing required.", {
        case ((_, hasEveningPricing, eveningBeginsAt, eveningPricePerHour, _,
          _)) =>

          !hasEveningPricing ||
          (eveningBeginsAt.isDefined && eveningPricePerHour.isDefined)
      }: (TupleType) => Boolean).
      verifying("Weekend pricing required.", {
        case ((_, _, _, _, hasWeekendPricing, weekendPricePerHour)) =>
          !hasWeekendPricing || weekendPricePerHour.isDefined
      }: (TupleType) => Boolean).
      transform(
        {
          case (pricePerHour, hasEveningPricing, eveningBeginsAt,
            eveningPricePerHour, hasWeekendPricing, weekendPricePerHour) => {

            val eveningPricing =
              if (hasEveningPricing) {
                Some(EveningPricingPolicy(eveningBeginsAt.get,
                  eveningPricePerHour.get))
              } else {
                None
              }

            val weekendPricing =
              if (hasWeekendPricing) {
                Some(WeekendPricingPolicy(weekendPricePerHour.get))
              } else {
                None
              }

            PricingPolicy(pricePerHour, eveningPricing, weekendPricing)
          }
        },
        {
          case PricingPolicy(pricePerHour, eveningPricing, weekendPricing) => {
            (
              pricePerHour, eveningPricing.isDefined,
              eveningPricing.map(_.beginsAt),
              eveningPricing.map(_.pricePerHour),
              weekendPricing.isDefined, weekendPricing.map(_.pricePerHour)
            )
          }
        }: PricingPolicy => TupleType
      )
  }
}
