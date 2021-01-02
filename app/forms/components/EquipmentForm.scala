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

package forms.components

import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields
import models.{ Equipment, EquipmentPrice, EquipmentPricePerHour, EquipmentPricePerSession }

object EquipmentForm {

    type Data = Equipment

    def form(canUseEquipmentPrice: Boolean) = Form {
        // Default ID field to 0 (auto-increment) if not specified.
        val idField = optional(CustomFields.longId).
            transform(_.getOrElse(0L), (v: Long) => Some(v))

        val priceType = text.
            verifying(
                "Invalid price type", 
                { value => Seq("per-hour", "per-session").contains(value) })

        val price = mapping(
            "type"  -> priceType,
            "value" -> CustomFields.amount
        ) { 
            case ("per-hour", value) => EquipmentPricePerHour(value).asInstanceOf[EquipmentPrice]
            case ("per-session", value) => EquipmentPricePerSession(value)
            case (_, _) => throw new Exception("Unreachable path")
        } {
            case EquipmentPricePerHour(value) => Some(("per-hour", value))
            case EquipmentPricePerSession(value) => Some(("per-session", value))
        }

        mapping(
            "id"                -> idField,

            "category"          -> optional(CustomFields.equipmentCategory),
            "details"           -> CustomFields.optionalText,

            "price"             -> optional(price)
        )(Equipment.apply)(Equipment.unapply).
            // Prevents users to create new equipments with a price if this is not allowed by their
            // plan.
            verifying(
                "You can not specify a price for this equipment.",
                e => e.price.isEmpty || e.id != 0 || canUseEquipmentPrice)
    }
}
