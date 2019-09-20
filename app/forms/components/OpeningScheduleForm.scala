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

import java.time.LocalTime

import play.api.data.{ Form, FormError, Mapping }
import play.api.data.format.Formatter
import play.api.data.Forms._

import forms.CustomFields
import models.{ OpeningSchedule, OpeningTimes }

object OpeningScheduleForm {

  type Data = OpeningSchedule

  val form = Form(
    mapping(
      "monday"    -> openingTimes,
      "tuesday"   -> openingTimes,
      "wednesday" -> openingTimes,
      "thursday"  -> openingTimes,
      "friday"    -> openingTimes,
      "saturday"  -> openingTimes,
      "sunday"    -> openingTimes
    )(OpeningSchedule.apply)(OpeningSchedule.unapply))

  private def openingTimes: Mapping[Option[OpeningTimes]] = {
    type TupleType = (Boolean, Option[LocalTime], Option[LocalTime])

    tuple(
      "is-open"   -> boolean,
      "opens-at"  -> optional(CustomFields.jodaLocalTime),
      "closes-at" -> optional(CustomFields.jodaLocalTime)
    ).
      verifying("Open and close times required.", {
        case ((isOpen, opensAt, closesAt)) =>
          !isOpen || (opensAt.isDefined && closesAt.isDefined)
      }: (TupleType) => Boolean).
      transform(
        {
          case (true, Some(opensAt), Some(closesAt)) =>
            Some(OpeningTimes(opensAt, closesAt))
          case (false, _, _) => None
          // Should never be reached because of hereabove constraint:
          case _ => None
        },
        {
          case Some(OpeningTimes(opensAt, closesAt)) =>
            (true, Some(opensAt), Some(closesAt))
          case None => (false, None, None)
        }
      )
  }
}
