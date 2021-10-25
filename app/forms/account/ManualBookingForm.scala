/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2021  Raphael Javaux <raphael@noisycamp.com>
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

import java.time.Instant

import play.api.Configuration
import play.api.data.{ Form, Mapping }
import play.api.data.Forms._

import forms.CustomFields
import forms.components.{ BookingRepeatForm, BookingTimesForm }
import models.{ BookingRepeat, BookingRepeatCount, BookingRepeatUntil, BookingTimes, Studio }

/** A new manual booking event. */
object ManualBookingForm {

    def form(now: Instant, studio: Studio)(implicit config: Configuration) = Form(
        mapping(
            "title"             -> nonEmptyText,
            "times"             -> BookingTimesForm.form(now, studio, isManualBooking=true).mapping,
            "repeat"            -> optional(BookingRepeatForm.form.mapping)
        )(Data.apply)(Data.unapply).
            verifying("Last repeated date is too far away.", { data =>
                data.repeat match {
                    case Some(repeat) => {
                        val maxBookingDate = studio.maxManualBookingDate(now)
                        repeat.latest(data.times.beginsAt.toLocalDate).isBefore(maxBookingDate)
                    }
                    case None => true
                }
            })
    )

    case class Data(
        title:                  String,
        times:                  BookingTimes,
        repeat:                 Option[BookingRepeat])
}
