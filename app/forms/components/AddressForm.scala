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

import models.Address
import forms.CustomFields

object AddressForm {

  val form = Form(
    mapping(
      "address-1" -> nonEmptyText,
      "address-2" -> CustomFields.optionalText,
      "zipcode" -> optional(nonEmptyText),
      "city" -> nonEmptyText,
      "state" -> optional(nonEmptyText),
      "country" -> CustomFields.country,
    )(Address.apply)(Address.unapply).
      verifying("Invalid state/province.", { address => {
        (address.country.states, address.stateCode) match {
          case (states, Some(stateCode)) if states.nonEmpty =>
            states.contains(stateCode)
          case (states, None) if states.isEmpty => true
          case _ => false
        }
      } }).
      verifying("Invalid zipcode.", { address => {
        address.country.hasZipCode == address.zipcode.isDefined
      } })
  )
}
