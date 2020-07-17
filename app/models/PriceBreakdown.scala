/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020 Raphael Javaux <raphaeljavaux@gmail.com>
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

import java.time.Duration

import squants.market

/** Contains the duration and price components of a booking. */
case class PriceBreakdown(
    durationTotal:        Duration,
    durationRegular:      Duration,
    durationEvening:      Duration,
    durationWeekend:      Duration,

    total:                market.Money,

    regularPricePerHour:  market.Money,
    eveningPricePerHour:  Option[market.Money],
    weekendPricePerHour:  Option[market.Money]) {

    require(durationTotal.equals(durationRegular plus durationEvening plus durationWeekend))

    def regularTotal: market.Money = {
        regularPricePerHour * durationAsHours(durationRegular)
    }

    def eveningTotal: Option[market.Money] = {
        eveningPricePerHour.map(_ * durationAsHours(durationEvening))
    }

    def weekendTotal: Option[market.Money] = {
        weekendPricePerHour.map(_ * durationAsHours(durationWeekend))
    }

    private def durationAsHours(duration: Duration): BigDecimal = {
        BigDecimal(durationRegular.getSeconds) / BigDecimal(3600.0)
    }
}
