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

  /** Validates the booking to the opening schedule.
   *
   * Returns `true` if and only if the studio is open during the selected
   * booking period. */
  def validateBooking(booking: BookingTimes): Boolean = {

    require(booking.duration.compareTo(Duration.ofDays(1)) <= 0)

    val beginsAt = booking.beginsAt.toLocalTime
    val endsAt = beginsAt.plus(booking.duration)

    val date = booking.beginsAt.toLocalDate

    val isOvernightBooking = // True if ends after 24:00 or later
      Duration.between(
        booking.beginsAt,
        booking.beginsAt.truncatedTo(ChronoUnit.DAYS).plusDays(1)
      ).compareTo(booking.duration) <= 0

    if (isOvernightBooking) {
      // If the booking spans on two days, the booking should matches the
      // opening times of the current day, and the current day's schedule should
      // allow overnights.

      openingTimes(date) match {
        case Some(times) if times.hasOvernight => {
          // opensAt <= beginsAt && endsAt <= closesAt
          !beginsAt.isBefore(times.opensAt) && !times.closesAt.isBefore(endsAt)
        }
        case _ => false
      }
    } else {
      // If the booking happens on a single day, it can either be the current
      // day, or an overnight from the day before.

      openingTimes(date) match {
                         // opensAt <= beginsAt
        case Some(times) if !beginsAt.isBefore(times.opensAt) => {
          // endsAt <= closesAt || hasOvernight
          !times.closesAt.isBefore(endsAt) || times.hasOvernight
        }
        case _ => {
          // Does not match the current day's schedule, tries with the day
          // before if it allows overnight.
          openingTimes(date.minusDays(1)) match {
            case Some(times) if times.hasOvernight => {
              // endsAt <= closesAt
              !times.closesAt.isBefore(endsAt)
            }
            case _ => false
          }
        }
      }
    }
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
