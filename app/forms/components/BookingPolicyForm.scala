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

import org.joda.time.{ Duration, LocalTime }
import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields

object BookingPolicyForm {

  val form = Form(
    mapping(
      "min-booking-duration"    -> CustomFields.seconds,
      "automatic-approval"      -> boolean,
      "can-cancel"              -> boolean,
      "cancellation-notice"     -> optional(CustomFields.seconds)
    )(Data.apply)(Data.unapply).
      verifying("Cancellation notice required.", { data =>
        !data.canCancel || data.cancellationNotice.isDefined}))

  case class Data(
    minBookingDuration:   Duration,
    automaticApproval:    Boolean,
    canCancel:            Boolean,
    cancellationNotice:   Option[Duration])
}
