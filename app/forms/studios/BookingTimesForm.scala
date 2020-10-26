/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2020  Raphael Javaux <raphael@noisycamp.com>
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

import java.time.{ Duration, Instant, LocalDate, LocalTime }

import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields
import models.{ BookingTimes, Studio }

/** A form to place a booking request. */
object BookingTimesForm {

    type Data = BookingTimes

    def form(studio: Studio)(implicit config: Configuration) = Form {
        val now = Instant.now
        val minBookingDateTime = studio.currentDateTime(now)
        val maxBookingDate = studio.maxBookingDate(now)

        mapping(
            "begins-at"       -> CustomFields.localDateTime.
                verifying(
                    "A booking cannot start in the past.",
                    beginsAt => !minBookingDateTime.isAfter(beginsAt)).
                verifying(
                    "A booking cannot be too far in the future.",
                    beginsAt => beginsAt.toLocalDate.isBefore(maxBookingDate)),
            "duration"        -> CustomFields.seconds.
                verifying(
                    "Less than the minimal rental duration set by the studio's manager.",
                    duration => duration.compareTo(studio.bookingPolicy.minBookingDuration) >= 0).
                verifying(
                    "Can't exceed 24 hours.",
                    duration => duration.compareTo(Duration.ofDays(1)) <= 0)
            )(BookingTimes.apply)(BookingTimes.unapply).
                verifying(
                    "The studio is not open during the selected booking period.",
                    booking => studio.openingSchedule.
                        validateBooking(studio.pricingPolicy, booking).
                        isDefined)
    }
}
