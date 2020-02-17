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

case class OpeningSchedule(
  monday:     Option[OpeningTimes],
  tuesday:    Option[OpeningTimes],
  wednesday:  Option[OpeningTimes],
  thursday:   Option[OpeningTimes],
  friday:     Option[OpeningTimes],
  saturday:   Option[OpeningTimes],
  sunday:     Option[OpeningTimes]) {

  /** Returns the daily opening schedule for the given booking date. */
  def openingTimes(date: LocalDate): Option[(LocalDateTime, LocalDateTime)] = {
    val times = date.getDayOfWeek match {
      case DayOfWeek.MONDAY => monday
      case DayOfWeek.TUESDAY => tuesday
      case DayOfWeek.WEDNESDAY => wednesday
      case DayOfWeek.THURSDAY => thursday
      case DayOfWeek.FRIDAY => friday
      case DayOfWeek.SATURDAY => saturday
      case DayOfWeek.SUNDAY => sunday
    }

    times.map { _.atDate(date) }
  }

  /** Validates the booking to the opening schedule.
   *
   * Returns `true` if and only if the studio is open during the selected
   * booking period. */
  def validateBooking(booking: BookingTimes): Boolean = {

    val beginsAt = booking.beginsAt
    val endsAt = beginsAt.plus(booking.duration)

    val date = beginsAt.toLocalDate

    def matchOpeningTimes(opensAt: LocalDateTime, closesAt: LocalDateTime) = {
      // opensAt <= beginsAt && endsAt <= closesAt
      !beginsAt.isBefore(opensAt) && !endsAt.isBefore(endsAt)
    }

    openingTimes(date) match {
      case Some((opensAt, closesAt)) if matchOpeningTimes(opensAt, closesAt) =>
        true
      case _ => {
        // Is yesterday an overnight ?
        val yesterday = date.minusDays(1)

        openingTimes(yesterday) match {
          case Some((opensAt, closesAt)) if matchOpeningTimes(opensAt, closesAt)
            => true
          case _ => false
        }
      }
    }
  }
}

case class OpeningTimes(
  opensAt:  LocalTime,
  closesAt: LocalTime) {

  /** Returns `true` if the studio closes on the next day's early morning. */
  def hasOvernight = closesAt.isBefore(opensAt)

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

  /** Returns `true` if the booked period matches opening times for that day. */
  def validateBooking(beginsAt: LocalTime, duration: Duration): Boolean = {
    // Can't last more than 24h.
    require(duration.compareTo(Duration.ofDays(1)) <= 0)

    lazy val endsAt = beginsAt.plus(duration)

    if (hasOvernight) {
      if (beginsAt.isBefore(opensAt)) {
        // Booking starts in the early morning.

        // beginsAt <= endsAt && endsAt <= closesAt
        !endsAt.isBefore(beginsAt) && !closesAt.isBefore(endsAt)
      } else {
        // Booking starts during the day but might stop in the early morning

        // endsAt >= opensAt || endsAt <= closesAt
        !endsAt.isBefore(opensAt) || !closesAt.isBefore(endsAt)
      }
    } else {
      // Booking starts during the day and must stop on the same day.

      // opensAt <= beginsAt && endsAt <= closesAt
      !beginsAt.isBefore(opensAt) && !closesAt.isBefore(endsAt)
    }
  }
}
