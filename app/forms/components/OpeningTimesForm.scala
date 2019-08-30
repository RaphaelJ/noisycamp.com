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
import play.api.data.{ Form, FormError, Mapping }
import play.api.data.format.Formatter
import play.api.data.Forms._

import forms.CustomFields

object OpeningTimesForm {

  val form = Form(
    mapping(
      "monday"    -> openingTimes,
      "tuesday"   -> openingTimes,
      "wednesday" -> openingTimes,
      "thursday"  -> openingTimes,
      "friday"    -> openingTimes,
      "saturday"  -> openingTimes,
      "sunday"    -> openingTimes
    )(Data.apply)(Data.unapply))

  case class Data(
    monday:     OpeningTimes,
    tuesday:    OpeningTimes,
    wednesday:  OpeningTimes,
    thursday:   OpeningTimes,
    friday:     OpeningTimes,
    saturday:   OpeningTimes,
    sunday:     OpeningTimes)

  case class OpeningTimes(
    isOpen:   Boolean,
    opensAt:  Option[LocalTime],
    closesAt: Option[LocalTime])

  private def openingTimes: Mapping[OpeningTimes] = {
    mapping(
      "is-open"   -> boolean,
      "opens-at"  -> optional(CustomFields.jodaLocalTime),
      "closes-at" -> optional(CustomFields.jodaLocalTime)
    )(OpeningTimes.apply)(OpeningTimes.unapply).
      verifying("Open and close times required.", { openingTimes =>
          !openingTimes.isOpen ||
          (openingTimes.opensAt.isDefined && openingTimes.closesAt.isDefined)})
  }
}
