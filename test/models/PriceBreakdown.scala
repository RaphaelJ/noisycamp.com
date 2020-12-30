/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphaeljavaux@gmail.com>
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

import java.time.Duration

import org.scalatest.Matchers._
import org.scalatestplus.play._
import squants.market.{ CAD, Currency, EUR, Money, USD }

import models.{
    BookingDurations, LocalEquipmentPricePerHour, LocalEquipmentPricePerSession, PriceBreakdown }

class PriceBreakdownSpec extends PlaySpec {

    val equipmentPrices = Seq(
        LocalEquipmentPricePerHour(EUR(12)),
        LocalEquipmentPricePerSession(EUR(25)))

    val breakdown = PriceBreakdown(
        BookingDurations(Duration.ofHours(12), Duration.ZERO, Duration.ZERO),
        EUR(15), Some(EUR(20)), Some(EUR(25)), equipmentPrices, Some(0.05))
    val eveningBreakdown = PriceBreakdown(
        BookingDurations(Duration.ofMinutes(3 * 60 + 30), Duration.ofHours(4), Duration.ZERO),
        CAD(15), Some(CAD(20)), None, Seq.empty, Some(0.08))
    val weekendBreakdown = PriceBreakdown(
        BookingDurations(Duration.ZERO, Duration.ZERO, Duration.ofHours(8)),
        USD(15), Some(USD(20)), Some(USD(12)), Seq.empty, None)

    "PriceBreakdown" must {
        "work with a regular booking" in {
            breakdown.total should be (EUR(349))
            breakdown.totalRegular should be (EUR(180))
            breakdown.totalEvening should be (Some(EUR(0)))
            breakdown.totalWeekend should be (Some(EUR(0)))
            breakdown.totalEquipments should be (Some(EUR(169)))
            breakdown.transactionFee should be (Some(EUR(17.45)))
        }

        "work with a booking with an evening rate" in {
            eveningBreakdown.total should be (CAD(132.5))
            eveningBreakdown.totalRegular should be (CAD(52.5))
            eveningBreakdown.totalEvening should be (Some(CAD(80)))
            eveningBreakdown.totalWeekend should be (None)
            eveningBreakdown.transactionFee should be (Some(CAD(10.6)))
        }

        "work with a booking with a weekend rate" in {
            weekendBreakdown.total should be (USD(96))
            weekendBreakdown.totalRegular should be (USD(0))
            weekendBreakdown.totalEvening should be (Some(USD(0)))
            weekendBreakdown.totalWeekend should be (Some(USD(96)))
            weekendBreakdown.transactionFee should be (None)
        }
    }
}
