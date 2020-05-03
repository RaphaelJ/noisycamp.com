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

import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields
import models.Equipment

object EquipmentForm {

  type Data = Equipment

  val form = Form {
    // Default ID field to 0 (auto-increment) if not specified.
    val idField = optional(CustomFields.longId).
      transform(_.getOrElse(0L), (v: Long) => Some(v))

    mapping(
      "id"                      -> idField,
      "category"                -> optional(CustomFields.equipmentCategory),
      "details"                 -> CustomFields.optionalText,
      "price-per-hour"          -> optional(CustomFields.amount)
    )(Equipment.apply)(Equipment.unapply)
  }
}
