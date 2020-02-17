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

package test.models

import java.time.{ LocalDate, LocalTime, Duration }

import org.scalatest.Matchers._
import org.scalatestplus.play._

import models.{ BookingTimes, OpeningSchedule, OpeningTimes }

class OpeningScheduleSpec extends PlaySpec {

  "OpeningSchedule.validateBooking" must {
    "return false on a closed day" in {
      OpeningSchedule(None, None, None, None, None, None, None).
        validateBooking(BookingTimes(
          LocalDate.of(2019, 3, 6), LocalTime.of(13, 30),
          Duration.ofHours(4))) should be
        (None)
    }
    
    // "return false when the booking starts before the studio opens" in {
    //   OpeningSchedule(None, None, None, None,
    //     Some(OpeningTimes(LocalTime.of(18, 0), LocalTime.of(2, 30))),
    //     None, None).
    //     isStudioOpen(BookingTimes(
    //       LocalDate.of(2020, 2, 7), LocalTime.of(13, 30),
    //       Duration.ofHours(1))) should be
    //     (false)
    // }
    //
    // "return false when the booking finishes after the studio closes" in {
    //   OpeningSchedule(
    //     Some(OpeningTimes(LocalTime.of(12, 0), LocalTime.of(18, 30))),
    //     None, None, None, None, None, None).
    //     isStudioOpen(BookingTimes(
    //       LocalDate.of(2020, 2, 10), LocalTime.of(16, 30),
    //       Duration.ofHours(3))) should be
    //     (false)
    // }
    //
    // "return true when the booking finishes exactly when the studio closes" in {
    //   OpeningSchedule(None, None, None,
    //     Some(OpeningTimes(LocalTime.of(15, 0), LocalTime.of(2, 30))),
    //     None, None, None).
    //     isStudioOpen(BookingTimes(
    //       LocalDate.of(2020, 12, 31), LocalTime.of(22, 30),
    //       Duration.ofHours(3))) should be
    //     (true)
    // }
    //
    // "return true when the booking starts overnight on an open day" in {
    //   OpeningSchedule(None, None, None, None, None,
    //     Some(OpeningTimes(LocalTime.of(18, 0), LocalTime.of(4, 30))),
    //     None).
    //     isStudioOpen(BookingTimes(
    //       LocalDate.of(2020, 2, 6), LocalTime.of(0, 0),
    //       Duration.ofHours(2))) should be
    //     (true)
    // }
  }
}
