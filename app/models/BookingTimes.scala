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

package models

import java.time.{ LocalDateTime, Duration }

trait HasBookingTimes {
    def bookingTimes: BookingTimes
}

/** Time information related to a booking. */
case class BookingTimes(
    beginsAt:           LocalDateTime,
    duration:           Duration)
    extends HasBookingTimes {

    def endsAt: LocalDateTime = beginsAt plus duration

    def bookingTimes = this
}
