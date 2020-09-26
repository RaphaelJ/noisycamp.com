/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphael@noisycamp.com>
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

import java.time.{ Duration, LocalDateTime, LocalTime, ZoneId }

import org.scalatest.Matchers._
import org.scalatestplus.play._

import models.{ Address, BookingPolicy, BookingTimes, Coordinates, Location, OpeningSchedule,
    OpeningTimes, PaymentPolicy, PricingPolicy, Studio, StudioBooking, StudioBookingPaymentOnsite,
    StudioBookingStatus, User }
import i18n.Country

class StudioBookingSpec extends PlaySpec {

    private val user = User(
        id = 1,
        firstName = None, lastName = None,
        email = "davenul@test.com",
        avatarId = None)

    private val studio = Studio(
        id = 1,
        ownerId = user.id,
        published = true,
        name = "Test Studio",
        description = "Test Studio",
        usePractice = true, useRecording = false, useLive = false, useLessons = false,
        location = Location(
            Address("Rue du test", None, None, "Testingrad", None, Country.Australia),
            Coordinates(0, 0)),
        timezone = ZoneId.of("Australia/Darwin"),
        openingSchedule = OpeningSchedule(None, None, None,
            Some(OpeningTimes(LocalTime.of(8, 0), LocalTime.of(2, 30))), None, None, None),
        pricingPolicy = PricingPolicy(35, None, None),
        bookingPolicy = BookingPolicy(Duration.ZERO, true, None),
        paymentPolicy = PaymentPolicy(true, true))

    private val studioBooking = StudioBooking(
        studio = studio,
        customer = user,
        status = StudioBookingStatus.PendingValidation,
        cancellationPolicy = None,
        times = BookingTimes(LocalDateTime.of(2020, 10, 1, 10, 30), Duration.ofMinutes(90)),
        transactionFeeRate = None, payment = StudioBookingPaymentOnsite())
    
    private def getInstantAtStudioTimeZone(date: LocalDateTime) = {
        val offset = studio.timezone.getRules.getOffset(date)
        date.toInstant(offset)
    }

    "StudioBooking.isStarted" must {
        "returns false before a booking" in {
            studioBooking.isStarted(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 10, 1, 10, 0))
                ) should be (false)
        }

        "returns true during a booking" in {
            studioBooking.isStarted(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 10, 1, 10, 30))
                ) should be (true)
        }

        "returns true after a booking" in {
            studioBooking.isStarted(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 10, 1, 14, 30))
                ) should be (true)
        }
    }

    "StudioBooking.isOngoing" must {
        "returns false before a booking" in {
            studioBooking.isOngoing(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 9, 12, 23, 0))
                ) should be (false)
        }

        "returns true during a booking" in {
            studioBooking.isOngoing(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 10, 1, 11, 0))
                ) should be (true)
        }

        "returns false after a booking" in {
            studioBooking.isOngoing(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 12, 24, 15, 0))
                ) should be (false)
        }
    }

    "StudioBooking.isCompleted" must {
        "returns false before a booking" in {
            studioBooking.isCompleted(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 10, 1, 9, 59))
                ) should be (false)
        }

        "returns false during a booking" in {
            studioBooking.isCompleted(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 10, 1, 11, 59))
                ) should be (false)
        }

        "returns true after a booking" in {
            studioBooking.isCompleted(
                studio, getInstantAtStudioTimeZone(LocalDateTime.of(2020, 10, 1, 12, 0))
                ) should be (true)
        }
    }

    "StudioBooking.toHexString" must {
        "returns an hexadecimal representation of a byte sequence" in {
            StudioBooking.toHexString(Seq(179.toByte, 63.toByte, 58.toByte)) should be ("b33f3a")
        }
    }
}