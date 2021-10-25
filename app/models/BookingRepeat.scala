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

package models

import java.time.{ LocalDate, Period }
import views.html.tags.duration

object BookingRepeatFrequency extends Enumeration {

    case class Val(val value: String, val period: Period) extends super.Val

    val Daily = Val("daily", Period.ofDays(1))
    val Weekly = Val("weekly", Period.ofWeeks(1))
    val Monthly = Val("monthly", Period.ofMonths(1))
    val Yearly = Val("yearly", Period.ofYears(1))
}

sealed trait BookingRepeat {
    def frequency:                      BookingRepeatFrequency.Val

    def count(firstOn: LocalDate):     Int

    /** The last (inclusive) repeated event. */
    def until(firstOn: LocalDate):     LocalDate

    /** List all the dates of the repeated event starting from the provided date. */
    def dates(firstOn: LocalDate): Seq[LocalDate] = {
        (1 to count(firstOn)).
            map { i => firstOn.plus(frequency.period.multipliedBy(i - 1)) }
    }
}

/** Repeat an event a certain amount of time. */
final case class BookingRepeatCount(
    val frequency:          BookingRepeatFrequency.Val,
    val count:              Int,
    ) extends BookingRepeat {
    require(count >= 1)

    def count(firstOn: LocalDate) = count

    def until(firstOn: LocalDate) = {
        firstOn plus frequency.period.multipliedBy((count - 1))
    }
}

/** Repeat an event until a provided date (inclusive). */
final case class BookingRepeatUntil(
    val frequency:          BookingRepeatFrequency.Val,
    val until:              LocalDate,
    ) extends BookingRepeat {

    def count(firstOn: LocalDate): Int = {
        require(firstOn.isBefore(until) || firstOn.isEqual(until))

        val repeatPeriod = Period.between(firstOn, until.plusDays(1))

        // Period does not provide a `dividedBy` method. Do a binary search using `multipliedBy`.
        // Time complexity: O(log (2^16)) = O(1)
        def go(low: Int, high: Int): Int = {
            require(low <= high)

            val middle = (low + high) / 2

            if (middle == low) {
                assert(low == high || low + 1 == high)
                middle
            } else {
                val probe = firstOn.plus(frequency.period.multipliedBy(middle - 1))
                if (probe.isBefore(until) || probe.isEqual(until)) {
                    go(middle, high)
                } else {
                    go(low, middle)
                }
            }
        }

        go(1, Short.MaxValue)
    }

    def until(beginsOn: LocalDate) = until
}