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

import java.time.LocalDate

import org.scalatest.Matchers._
import org.scalatestplus.play._

import models.{ BookingRepeat, BookingRepeatCount, BookingRepeatFrequency, BookingRepeatUntil }
import java.time.LocalDate

class BookingRepeatSpec extends PlaySpec {

    "BookingRepeat" must {
        "return the valid event dates" in {
            BookingRepeatCount(BookingRepeatFrequency.Daily, 7).
                dates(LocalDate.of(2021, 10, 6)) should be (Seq(
                    LocalDate.of(2021, 10, 6),
                    LocalDate.of(2021, 10, 7),
                    LocalDate.of(2021, 10, 8),
                    LocalDate.of(2021, 10, 9),
                    LocalDate.of(2021, 10, 10),
                    LocalDate.of(2021, 10, 11),
                    LocalDate.of(2021, 10, 12)))

            BookingRepeatUntil(BookingRepeatFrequency.Monthly, LocalDate.of(2022, 3, 6)).
                dates(LocalDate.of(2021, 10, 6)) should be (Seq(
                    LocalDate.of(2021, 10, 6),
                    LocalDate.of(2021, 11, 6),
                    LocalDate.of(2021, 12, 6),
                    LocalDate.of(2022, 1, 6),
                    LocalDate.of(2022, 2, 6),
                    LocalDate.of(2022, 3, 6)))
        }
    }

    "BookingRepeatCount" must {
        "compute the `until` value correctly" in {
            BookingRepeatCount(BookingRepeatFrequency.Daily, 7).
                until(LocalDate.of(2021, 10, 6)) should be (LocalDate.of(2021, 10, 12))
            BookingRepeatCount(BookingRepeatFrequency.Yearly, 1).
                until(LocalDate.of(2021, 10, 6)) should be (LocalDate.of(2021, 10, 6))
        }
    }

    "BookingRepeatUntil" must {
        "compute the `count` value correctly" in {
            BookingRepeatUntil(BookingRepeatFrequency.Daily, LocalDate.of(2021, 10, 12)).
                count(LocalDate.of(2021, 10, 6)) should be (7)
            BookingRepeatUntil(BookingRepeatFrequency.Yearly, LocalDate.of(2021, 10, 6)).
                count(LocalDate.of(2021, 10, 6)) should be (1)
            BookingRepeatUntil(BookingRepeatFrequency.Monthly, LocalDate.of(2021, 12, 12)).
                count(LocalDate.of(2021, 10, 6)) should be (3)
            BookingRepeatUntil(BookingRepeatFrequency.Monthly, LocalDate.of(2021, 12, 5)).
                count(LocalDate.of(2021, 10, 6)) should be (2)
        }
    }
}
