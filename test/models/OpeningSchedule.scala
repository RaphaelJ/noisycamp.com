/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019-2020  Raphael Javaux <raphaeljavaux@gmail.com>
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

import java.time.{ LocalDateTime, LocalTime, Duration }

import org.scalatest.Matchers._
import org.scalatestplus.play._

import models.{
    BookingBreakdown, BookingTimes, EveningPricingPolicy, OpeningSchedule, OpeningTimes,
    PricingPolicy, WeekendPricingPolicy }

class OpeningScheduleSpec extends PlaySpec {

    // Defines useful pricing policy
    private val pricingPolicy = PricingPolicy(35, None, None)
    private val eveningPricingPolicy = PricingPolicy(
        25, Some(EveningPricingPolicy(LocalTime.of(18, 0), 30)), None)
    private val weekendPricingPolicy = PricingPolicy(35, None, Some(WeekendPricingPolicy(45)))
    private val eveningWeekendPricingPolicy = PricingPolicy(
        25, Some(EveningPricingPolicy(LocalTime.of(18, 30), 15)), Some(WeekendPricingPolicy(40)))

    "OpeningSchedule.validateBooking" must {
        "does not validate on a closed day" in {
            OpeningSchedule(None, None, None, None, None, None, None).
                validateBooking(pricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 7, 13, 30),   // Friday
                    Duration.ofHours(4))) should be (None)
        }

        "does not validate when the booking starts before the studio opens" in {
            OpeningSchedule(None, None, None, None,
                Some(OpeningTimes(LocalTime.of(18, 0), LocalTime.of(2, 30))), None, None).
                validateBooking(pricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 7, 13, 30),   // Friday
                    Duration.ofHours(1))) should be (None)
        }

        "does not validate when the booking finishes after the studio closes" in {
            OpeningSchedule(
                Some(OpeningTimes(LocalTime.of(12, 0), LocalTime.of(18, 30))),
                None, None, None, None, None, None).
                validateBooking(pricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 17, 16, 30),  // Monday
                    Duration.ofHours(3))) should be (None)
        }

        "validates when the booking finishes exactly when the studio closes" in {
            OpeningSchedule(None, None, None,
                Some(OpeningTimes(LocalTime.of(15, 0), LocalTime.of(2, 30))), None, None, None).
                validateBooking(eveningWeekendPricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 12, 31, 22, 30), // Thursday
                    Duration.ofHours(4))) should be (
                        Some(BookingBreakdown(Duration.ZERO, Duration.ofHours(4), Duration.ZERO)))
        }

        "validates when the booking starts overnight on an open day" in {
            OpeningSchedule(None, None, None, None, None,
                Some(OpeningTimes(LocalTime.of(18, 0), LocalTime.of(4, 30))), None).
                validateBooking(weekendPricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 9, 0, 0),     // Sunday morning
                    Duration.ofHours(2))) should be (
                        Some(BookingBreakdown(Duration.ZERO, Duration.ZERO, Duration.ofHours(2))))

            OpeningSchedule(
                Some(OpeningTimes(LocalTime.of(20, 0), LocalTime.of(23, 30))),
                None, None, None, None, None,
                Some(OpeningTimes(LocalTime.of(18, 0), LocalTime.of(4, 0)))).
                validateBooking(eveningWeekendPricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 10, 1, 0),     // Monday morning
                    Duration.ofHours(3))) should be (
                        Some(BookingBreakdown(Duration.ZERO, Duration.ZERO, Duration.ofHours(3))))
        }

        "validates when the booking ends on the next day during an overnight" in {
            OpeningSchedule(
                Some(OpeningTimes(LocalTime.of(18, 0), LocalTime.of(0, 0))),
                None, None, None, None, None,
                Some(OpeningTimes(LocalTime.of(18, 0), LocalTime.of(7, 0)))).
                validateBooking(pricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 23, 18, 0),   // Sunday evening
                    Duration.ofHours(7))) should be (
                        Some(BookingBreakdown(Duration.ofHours(7), Duration.ZERO, Duration.ZERO)))
        }

        "handles 24 opening shedules" in {
            OpeningSchedule(
                Some(OpeningTimes(LocalTime.of(0, 0), LocalTime.of(0, 0))),
                None, None, None, None, None, None).
                validateBooking(eveningWeekendPricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 24, 0, 0),    // Monday
                    Duration.ofHours(24))) should be (
                        Some(BookingBreakdown(
                            Duration.ofMinutes(18 * 60 + 30), Duration.ofMinutes(5 * 60 + 30),
                            Duration.ZERO)))

            OpeningSchedule(
                Some(OpeningTimes(LocalTime.of(0, 0), LocalTime.of(0, 0))),
                None, None, None, None, None, None).
                validateBooking(eveningPricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 24, 7, 0),    // Monday
                    Duration.ofHours(23))) should be (None)

            OpeningSchedule(
                Some(OpeningTimes(LocalTime.of(7, 0), LocalTime.of(7, 0))),
                None, None, None, None, None, None).
                validateBooking(eveningPricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 24, 10, 0),    // Monday
                    Duration.ofHours(21))) should be (
                        Some(BookingBreakdown(
                            Duration.ofHours(8), Duration.ofHours(13), Duration.ZERO)))

            OpeningSchedule(
                Some(OpeningTimes(LocalTime.of(16, 0), LocalTime.of(16, 0))),
                None, None, None, None, None, None).
                validateBooking(pricingPolicy, BookingTimes(
                    LocalDateTime.of(2020, 2, 24, 20, 0),    // Monday
                    Duration.ofHours(23))) should be (None)
        }
    }
}

class OpeningTimesSpec extends PlaySpec {

    "OpeningTimes.hasOvernight" must {
        "be false if the studio closes during the evening" in {
            OpeningTimes(LocalTime.of(12, 0), LocalTime.of(22, 0)).
                hasOvernight should be (false)
        }

        "be true if the studio closes during the early morning" in {
            OpeningTimes(LocalTime.of(20, 0), LocalTime.of(2, 30)).
                hasOvernight should be (true)
        }

        "be true for 24h opening times" in {
            OpeningTimes(LocalTime.of(0, 0), LocalTime.of(0, 0)).
                hasOvernight should be (true)

            OpeningTimes(LocalTime.of(8, 0), LocalTime.of(8, 0)).
                hasOvernight should be (true)
        }
    }
}
