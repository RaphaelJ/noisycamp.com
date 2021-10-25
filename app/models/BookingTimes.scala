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
import org.joda.time
import akka.actor.ProviderSelection

/** Time information related to a booking. */
trait HasBookingTimes {
    def beginsAt:       LocalDateTime
    def duration:       Duration
}

case class BookingTimes(
    beginsAt:           LocalDateTime,
    duration:           Duration)
    extends HasBookingTimes {

    require(!duration.isNegative && !duration.isZero)

    def endsAt: LocalDateTime = withRepeat(None).endsAt

    def withRepeat(repeat: Option[BookingRepeat]): BookingTimesWithRepeat = {
        BookingTimesWithRepeat(beginsAt, duration, repeat)
    }
}

/** Time information related to a booking, with repeat information. */
case class BookingTimesWithRepeat(
    beginsAt:           LocalDateTime,
    duration:           Duration,
    repeat:             Option[BookingRepeat])
    extends HasBookingTimes {

    require(!duration.isNegative && !duration.isZero)

    /** The (exclusive) date of the end of the latest date of the selected times.
     *
     * Time complexity: O(1). */
    def endsAt: LocalDateTime = {
        val latestBeginsAt =
            repeat match {
                case Some(bookingRepeat) => {
                    LocalDateTime.of(
                        bookingRepeat.latest(beginsAt.toLocalDate),
                        beginsAt.toLocalTime)
                }
                case None => beginsAt
            }

        latestBeginsAt plus duration
    }

    /** List all the begin and end times (inclusive and exclusive, respectively) of the repeated
     * events. */
    def times: Seq[(LocalDateTime, LocalDateTime)] = {
        repeat match {
            case Some(repeatValue) => {
                val beginsAtTime = beginsAt.toLocalTime
                repeatValue.
                    dates(beginsAt.toLocalDate).
                    map { date =>
                        val beginsAt = LocalDateTime.of(date, beginsAtTime)
                        val endsAt = beginsAt plus duration
                        (beginsAt, endsAt)
                    }
            }
            case None => Seq((beginsAt, beginsAt plus duration))
        }
    }

    /** Returns true if one of the repeated event of the booking times overlap.
     *
     * Time complexity: O(n) where `n` is the number of repeated events. */
    def hasOverlap(other: BookingTimesWithRepeat): Boolean = {
        var thisTimes = times
        var otherTimes = other.times

        while (thisTimes.nonEmpty && otherTimes.nonEmpty) {
            val thisTime = thisTimes.head
            val otherTime = otherTimes.head

            if (thisTime._2.isAfter(otherTime._1) && thisTime._1.isBefore(otherTime._2)) {
                return true
            } else if (thisTime._1.isBefore(otherTime._1)) {
                thisTimes = thisTimes.tail
            } else {
                assert(thisTime._1.isAfter(otherTime._1))
                otherTimes = otherTimes.tail
            }
        }

        false
    }
}
