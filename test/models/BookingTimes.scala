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

package test.models

import java.time.{ Duration, LocalDateTime }

import org.scalatest.Matchers._
import org.scalatestplus.play._

import models.{
    BookingTimes, BookingRepeat, BookingTimesWithRepeat, BookingRepeatCount, BookingRepeatFrequency,
    BookingRepeatUntil }

class BookingTimesSpec extends PlaySpec {

    "BookingTimes" must {
        val times = BookingTimes(
            LocalDateTime.of(2021, 10, 14, 0, 0),
            Duration.ofHours(24))

        val timesWithRepeat = BookingTimesWithRepeat(
            LocalDateTime.of(2021, 10, 14, 23, 30),
            Duration.ofHours(1),
            Some(BookingRepeatCount(BookingRepeatFrequency.Daily, 2)))

        "return the valid `endsAt` value" in {
            times.endsAt should be (LocalDateTime.of(2021, 10, 15, 0, 0))
            timesWithRepeat.endsAt should be (LocalDateTime.of(2021, 10, 16, 0, 30))
        }
    }
}
