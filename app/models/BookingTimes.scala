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

/** Time information related to a booking. */
trait HasBookingTimes {
    def beginsAt:       LocalDateTime
    def duration:       Duration

    require(!duration.isNegative && !duration.isZero)

    def endsAt: LocalDateTime = beginsAt plus duration

    def hasOverlap(other: HasBookingTimes): Boolean = {
        beginsAt.isBefore(other.endsAt) && endsAt.isAfter(other.beginsAt)
    }
}

object HasBookingTimes {
    implicit def ordering[A <: HasBookingTimes] = new Ordering[A] {
        def compare(a: A, b: A) = a.beginsAt.compareTo(b.beginsAt)
    }
}

case class BookingTimes(
    val beginsAt:       LocalDateTime,
    val duration:       Duration)
    extends HasBookingTimes {

    def withRepeat(repeat: Option[BookingRepeat]): BookingTimesWithRepeat = {
        BookingTimesWithRepeat(beginsAt, duration, repeat)
    }

    def repeatOnce = withRepeat(None)
}

/** Time information related to a booking, with repeat information. */
case class BookingTimesWithRepeat(
    val beginsAt:       LocalDateTime,
    val duration:       Duration,

    /** If `None`, only repeat the booking once. */
    val repeat:         Option[BookingRepeat]) {

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

    /** List all the booking times of this repeated event. */
    def times: Seq[BookingTimes] = {
        repeat match {
            case Some(repeatValue) => {
                val beginsAtTime = beginsAt.toLocalTime
                repeatValue.
                    dates(beginsAt.toLocalDate).
                    map { date =>
                        BookingTimes(LocalDateTime.of(date, beginsAtTime), duration)
                    }
            }
            case None => Seq(BookingTimes(beginsAt, duration))
        }
    }

    /** Returns true if one of the repeated event of the booking times overlap.
     *
     * Time complexity: O(n log(n)) where `n` is the number of repeated events. */
    def hasOverlap(other: BookingTimesWithRepeat): Boolean = {
        var thisTimes = times.sorted
        var otherTimes = other.times.sorted

        while (thisTimes.nonEmpty && otherTimes.nonEmpty) {
            val thisTime = thisTimes.head
            val otherTime = otherTimes.head

            if (thisTime.hasOverlap(otherTime)) {
                assert(beginsAt.isBefore(other.endsAt) && endsAt.isAfter(other.beginsAt))
                return true
            } else if (thisTime.beginsAt.isBefore(otherTime.beginsAt)) {
                thisTimes = thisTimes.tail
            } else {
                assert(thisTime.beginsAt.isAfter(otherTime.beginsAt))
                otherTimes = otherTimes.tail
            }
        }

        false
    }

    /** Returns the total duration of all the repeated booking events.
     *
     * Time complexity: O(1). */
    def totalDuration: Duration = {
        val count = repeat.map(_.count(beginsAt.toLocalDate)).getOrElse(1)
        duration multipliedBy count.toLong
    }
}

object BookingTimesWithRepeat {
    implicit def ordering = new Ordering[BookingTimesWithRepeat] {
        def compare(a: BookingTimesWithRepeat, b: BookingTimesWithRepeat) = {
            a.beginsAt.compareTo(b.beginsAt)
        }
    }
}

