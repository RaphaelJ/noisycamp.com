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

import java.time.{ Duration, LocalDate, LocalDateTime }

import org.scalatest.Matchers._
import org.scalatestplus.play._

import models.{
    BookingTimes, BookingRepeat, BookingTimesWithRepeat, BookingRepeatCount, BookingRepeatFrequency,
    BookingRepeatUntil }
import views.html.helper.repeat

class BookingTimesSpec extends PlaySpec {

    "BookingTimes" must {
        val times = BookingTimes(
            LocalDateTime.of(2021, 10, 14, 0, 0),
            Duration.ofHours(24))

        "return the valid `endsAt` value" in {
            times.endsAt should be (LocalDateTime.of(2021, 10, 15, 0, 0))
        }

        "detect overlaps correctly" in {
            times.hasOverlap(times) should be (true)

            // Starts before but ends before.
            times.hasOverlap(BookingTimes(
                LocalDateTime.of(2021, 10, 13, 0, 0),
                Duration.ofHours(24))) should be (false)

            // Starts before but ends during.
            times.hasOverlap(BookingTimes(
                LocalDateTime.of(2021, 10, 13, 1, 0),
                Duration.ofHours(24))) should be (true)

            // Starts during and ends during.
            times.hasOverlap(BookingTimes(
                LocalDateTime.of(2021, 10, 14, 10, 0),
                Duration.ofHours(2))) should be (true)

            // Starts during and ends after.
            times.hasOverlap(BookingTimes(
                LocalDateTime.of(2021, 10, 14, 20, 0),
                Duration.ofHours(12))) should be (true)

            // Starts after.
            times.hasOverlap(BookingTimes(
                LocalDateTime.of(2021, 10, 15, 0, 0),
                Duration.ofHours(12))) should be (false)
        }
    }

    "BookingTimesWithRepeat" must {
        val times = BookingTimesWithRepeat(
            LocalDateTime.of(2021, 10, 14, 23, 30),
            Duration.ofHours(1),
            Some(BookingRepeatCount(BookingRepeatFrequency.Daily, 2)))

        val timeWithoutRepeat = times.copy(repeat=None)

        "return the valid `endsAt` value" in {
            times.endsAt should be (LocalDateTime.of(2021, 10, 16, 0, 30))
            timeWithoutRepeat.endsAt should be (LocalDateTime.of(2021, 10, 15, 0, 30))
        }

        "lists the repeated times correctly" in {
            times.times should be (Seq(
                BookingTimes(LocalDateTime.of(2021, 10, 14, 23, 30), Duration.ofHours(1)),
                BookingTimes(LocalDateTime.of(2021, 10, 15, 23, 30), Duration.ofHours(1))))

            timeWithoutRepeat.times should be (Seq(
                BookingTimes(LocalDateTime.of(2021, 10, 14, 23, 30), Duration.ofHours(1))))
        }

        "detect overlaps correctly" in {
            times.hasOverlap(times) should be (true)

            // Does not conflict
            times.hasOverlap(BookingTimesWithRepeat(
                LocalDateTime.of(2021, 5, 13, 0, 0),
                Duration.ofHours(12),
                Some(BookingRepeatUntil(BookingRepeatFrequency.Weekly, LocalDate.of(2021, 9, 1)))
                )) should be (false)

            // Occurs during overlapping days, but with no overlaps.
            times.hasOverlap(BookingTimesWithRepeat(
                LocalDateTime.of(2021, 9, 14, 12, 0),
                Duration.ofHours(3),
                Some(BookingRepeatCount(BookingRepeatFrequency.Monthly, 6))
                )) should be (false)

            // 2nd repeat of `times` overlaps 3rd repeat of the parameterized instance.
            times.hasOverlap(BookingTimesWithRepeat(
                LocalDateTime.of(2019, 10, 15, 20, 0),
                Duration.ofHours(4),
                Some(BookingRepeatCount(BookingRepeatFrequency.Yearly, 4))
                )) should be (true)

            // 1st repeat of `times` overlaps a non-repeated parameterized instance.
            times.hasOverlap(BookingTimesWithRepeat(
                LocalDateTime.of(2021, 10, 15, 0, 15),
                Duration.ofHours(4),
                None)) should be (true)
        }

        "compute the total duration correctly" in {
            times.totalDuration should be (Duration.ofHours(2))
            timeWithoutRepeat.totalDuration should be (Duration.ofHours(1))
        }
    }
}
