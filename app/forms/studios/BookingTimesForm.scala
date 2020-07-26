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

package forms.studios

import java.time.{ Duration, LocalDate, LocalTime }

import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields
import models.{ BookingTimes, Studio }

/** A form to place a booking request. */
object BookingTimesForm {

  type Data = BookingTimes

  def form(studio: Studio) = Form {

    mapping(
      "begins-at"       -> CustomFields.localDateTime,
      "duration"        -> CustomFields.seconds.
        verifying(
          "Less than the minimal rental duration set by the studio's manager.",
          duration =>
            duration.compareTo(studio.bookingPolicy.minBookingDuration) >= 0
        ).
        verifying(
          "Can't exceed 24 hours.",
          duration => duration.compareTo(Duration.ofDays(1)) <= 0
        )
    )(BookingTimes.apply)(BookingTimes.unapply).
      verifying(
        "The studio is not open during the selected booking period.",
        booking => studio.openingSchedule.validateBooking(studio.pricingPolicy, booking).isDefined
      )
  }
}
