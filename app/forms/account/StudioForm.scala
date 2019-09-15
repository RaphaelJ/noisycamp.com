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

package forms.account

import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields
import models.Picture

/** A form to create and edit studios. */
object StudioForm {

  val form = Form(
    mapping(
      "general-info.name" -> nonEmptyText,
      "general-info.description" -> nonEmptyText,

      "location" -> forms.components.AddressForm.form.mapping,
      "opening-times" -> forms.components.OpeningTimesForm.form.mapping,
      "pricing" -> forms.components.PricingForm.form.mapping,
      "booking-policy" -> forms.components.BookingPolicyForm.form.mapping,
      "picures" -> seq(CustomFields.pictureId)
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    // General info
    name:         String,
    description:  String,

    location:       forms.components.AddressForm.Data,
    openingTimes:   forms.components.OpeningTimesForm.Data,
    pricing:        forms.components.PricingForm.Data,
    bookingPolicy:  forms.components.BookingPolicyForm.Data,
    pictures:       Seq[Picture#Id])
}
