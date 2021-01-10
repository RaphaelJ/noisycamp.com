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

import java.time.{ DayOfWeek, Duration, LocalDate, LocalDateTime, LocalTime }
import java.time.temporal.ChronoUnit

case class OpeningSchedule(
    monday:     Option[OpeningTimes],
    tuesday:    Option[OpeningTimes],
    wednesday:  Option[OpeningTimes],
    thursday:   Option[OpeningTimes],
    friday:     Option[OpeningTimes],
    saturday:   Option[OpeningTimes],
    sunday:     Option[OpeningTimes]) {

    /** Returns the daily opening schedule for the given booking date. */
    def openingTimes(date: LocalDate): Option[OpeningTimes] = {
        date.getDayOfWeek match {
            case DayOfWeek.MONDAY => monday
            case DayOfWeek.TUESDAY => tuesday
            case DayOfWeek.WEDNESDAY => wednesday
            case DayOfWeek.THURSDAY => thursday
            case DayOfWeek.FRIDAY => friday
            case DayOfWeek.SATURDAY => saturday
            case DayOfWeek.SUNDAY => sunday
        }
    }

    /** Validates the booking to the opening schedule and computes the time components of it.
     *
     * If the booking is not valid, return `None`.
     */
    def validateBooking(pricingPolicy: PricingPolicy, booking: BookingTimes):
        Option[BookingDurations] = {

        require(booking.duration.compareTo(Duration.ofDays(1)) <= 0)

        val beginsAt = booking.beginsAt.toLocalTime
        val endsAt = beginsAt.plus(booking.duration)

        val date = booking.beginsAt.toLocalDate

        val isOvernightBooking = // True if ends after 24:00 or later
            Duration.between(
                booking.beginsAt,
                booking.beginsAt.truncatedTo(ChronoUnit.DAYS).plusDays(1)).
                compareTo(booking.duration) <= 0

        if (isOvernightBooking) {
            // If the booking spans on two days, the booking should matches the
            // opening times of the current day, and the current day's schedule should
            // allow overnights.

            openingTimes(date) match {
                case Some(times) if
                    times.hasOvernight &&
                    // opensAt <= beginsAt && endsAt <= closesAt
                    !beginsAt.isBefore(times.opensAt) && !times.closesAt.isBefore(endsAt) => {

                    Some(toDurations(pricingPolicy, times, date, booking))
                }
                case _ => None
            }
        } else {
            // If the booking happens on a single day, it can either be the current
            // day, or an overnight from the day before.

            openingTimes(date) match {
                // opensAt <= beginsAt
                case Some(times) if !beginsAt.isBefore(times.opensAt) => {
                    // endsAt <= closesAt || hasOvernight
                    if (!times.closesAt.isBefore(endsAt) || times.hasOvernight) {
                        Some(toDurations(pricingPolicy, times, date, booking))
                    } else {
                        None
                    }
                }
                case _ => {
                    // Does not match the current day's schedule, tries with the day
                    // before if it allows overnight.
                    val yesterday = date.minusDays(1)

                    openingTimes(yesterday) match {
                        case Some(times) if
                            times.hasOvernight &&
                            // endsAt <= closesAt
                            !times.closesAt.isBefore(endsAt) => {

                            Some(toDurations(pricingPolicy, times, yesterday, booking))
                        }
                        case _ => None
                    }
                }
            }
        }
    }

    /** Contructs a BookingDurations object for the booking on the provided opened day. */
    private def toDurations(
        pricingPolicy: PricingPolicy, times: OpeningTimes, date: LocalDate,
        booking: BookingTimes): BookingDurations = {

        lazy val weekDay = date.getDayOfWeek
        lazy val isWeekend = weekDay == DayOfWeek.SATURDAY || weekDay == DayOfWeek.SUNDAY

        if (pricingPolicy.weekend.isDefined && isWeekend) {
            BookingDurations(Duration.ZERO, Duration.ZERO, booking.duration)
        } else if (pricingPolicy.evening.isDefined) {
            val eveningPolicy = pricingPolicy.evening.get

            val eveningBeginsAt =
                if (times.opensAt.isAfter(eveningPolicy.beginsAt)) {
                    // Evening pricing begins overnight.
                    date.plusDays(1).atTime(eveningPolicy.beginsAt)
                } else {
                    date.atTime(eveningPolicy.beginsAt)
                }

            if (booking.beginsAt.isBefore(eveningBeginsAt)) {
                val endsAt = booking.beginsAt.plus(booking.duration)

                val regularDuration =
                    if (endsAt.isBefore(eveningBeginsAt)) {
                        booking.duration
                    } else {
                        Duration.between(booking.beginsAt, eveningBeginsAt)
                    }

                val eveningDuration = booking.duration.minus(regularDuration)

                assert(!regularDuration.isNegative && !regularDuration.isZero)
                assert(!eveningDuration.isNegative)

                BookingDurations(regularDuration, eveningDuration, Duration.ZERO)
            } else {
                BookingDurations(Duration.ZERO, booking.duration, Duration.ZERO)
            }

        } else {
            BookingDurations(booking.duration, Duration.ZERO, Duration.ZERO)
        }
    }

    def toMap: Map[DayOfWeek, Option[OpeningTimes]] = {
        Map(
            DayOfWeek.MONDAY -> monday,
            DayOfWeek.TUESDAY -> tuesday,
            DayOfWeek.WEDNESDAY -> wednesday,
            DayOfWeek.THURSDAY -> thursday,
            DayOfWeek.FRIDAY -> friday,
            DayOfWeek.SATURDAY -> saturday,
            DayOfWeek.SUNDAY -> sunday)
    }
}

case class OpeningTimes(
    opensAt:  LocalTime,
    closesAt: LocalTime) {

    /** Returns `true` if the studio closes on the next day's early morning. */
    def hasOvernight = !opensAt.isBefore(closesAt) // closesAt <= opensAt

    /** Returns the opening and closing date-times as if the studio was opening
     * on the provided day. */
    def atDate(date: LocalDate): (LocalDateTime, LocalDateTime) = {
        val opensAtDt = opensAt.atDate(date)
        val closesAtDt =
        if (hasOvernight) {
            closesAt.atDate(date.plusDays(1))
        } else {
            closesAt.atDate(date)
        }

        assert(opensAtDt.isBefore(closesAtDt) || opensAtDt.isEqual(closesAtDt))

        (opensAtDt, closesAtDt)
    }
}
