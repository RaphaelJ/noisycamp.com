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

import org.joda.time.Duration
import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields
import models.{ BookingPolicy, CancellationPolicy }

object BookingPolicyForm {

  type Data = BookingPolicy

  val form = Form {
    type TupleType = (Duration, Boolean, Boolean, Option[Duration])

    tuple(
      "min-booking-duration"    -> CustomFields.seconds,
      "automatic-approval"      -> boolean,
      "can-cancel"              -> boolean,
      "cancellation-notice"     -> optional(CustomFields.seconds)
    ).
      verifying("Cancellation notice required.", {
        case (_, _, canCancel, cancellationNotice) =>
          !canCancel || cancellationNotice.isDefined
      }: (TupleType) => Boolean).
      transform(
        {
          case (minBookingDuration, automaticApproval, canCancel,
            cancellationNotice) =>

          val cancellationPolicy =
            if (canCancel) {
              cancellationNotice.map(CancellationPolicy(_))
            } else {
              None
            }

          BookingPolicy(minBookingDuration, automaticApproval,
            cancellationPolicy)
        },
        {
          case policy => (
            policy.minBookingDuration, policy.automaticApproval,
            policy.cancellationPolicy.isDefined,
            policy.cancellationPolicy.map(_.notice),
          )
        }: BookingPolicy => TupleType
      )
  }
}
