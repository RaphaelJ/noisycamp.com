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
import forms.components.PaymentPolicyForm
import models.{
  BookingPolicy, Equipment, Location, OpeningSchedule, Picture, PricingPolicy }

/** A form to create and edit studios. */
object StudioForm {

  val form = Form(
    mapping(
      "general-info.name" -> nonEmptyText,
      "general-info.description" -> nonEmptyText,

      "location" -> forms.components.LocationForm.form.mapping,
      "opening-schedule" -> forms.components.OpeningScheduleForm.form.mapping,
      "pricing-policy" -> forms.components.PricingPolicyForm.form.mapping,
      "booking-policy" -> forms.components.BookingPolicyForm.form.mapping,
      "payment-policy" -> forms.components.PaymentPolicyForm.form.mapping,

      "equipments" -> seq(forms.components.EquipmentForm.form.mapping),
      "pictures" -> seq(CustomFields.pictureId)
    )(Data.apply)(Data.unapply).
      verifying("PLESE REMOVE", _ => false))

  case class Data(
    name:             String,
    description:      String,

    location:         Location,
    openingSchedule:  OpeningSchedule,
    pricingPolicy:    PricingPolicy,
    bookingPolicy:    BookingPolicy,
    paymentPolicy:    PaymentPolicyForm.Data,

    equipments:       Seq[Equipment],
    pictures:         Seq[Picture#Id])
}
