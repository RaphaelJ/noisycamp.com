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

package models

import java.time.Duration

/** Provides the various duration components of a booking. */
case class BookingDurations(
    regular:        Duration,   // The time the booking is not subject to a special rate.
    evening:        Duration,
    weekend:        Duration) {

    def total: Duration = regular plus evening plus weekend

    def plus(other: BookingDurations) = {
        val sum = BookingDurations(
            regular plus other.regular,
            evening plus other.evening,
            weekend plus other.weekend)

        assert(sum.total == (total plus other.total))

        sum
    }
}
