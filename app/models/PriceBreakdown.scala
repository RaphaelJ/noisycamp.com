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

/** Contains the duration and pricing policy applied to a booking, and provides with helpers to
 * compute the actual monetary values of a booking. */
case class PriceBreakdown(
    durations:              BookingDurations,

    pricePerHour:           market.Money,
    pricePerHourEvening:    Option[market.Money],
    pricePerHourWeekend:    Option[market.Money],

    // The rate at which the NoisyCamp transaction fee will be computed.
    transactionFeeRate:     Option[BigDecimal]) {

    require(durations.evening.isZero || pricePerHourEvening.isDefined)
    require(durations.weekend.isZero || pricePerHourWeekend.isDefined)

    lazy val total: market.Money = {
        totalRegular + totalEvening.getOrElse(moneyZero) + totalWeekend.getOrElse(moneyZero)
    }

    lazy val totalRegular: market.Money = pricePerHour * durationAsHours(durations.regular)

    lazy val totalEvening: Option[market.Money] = {
        pricePerHourEvening.map(_ * durationAsHours(durations.evening))
    }

    lazy val totalWeekend: Option[market.Money] = {
        pricePerHourWeekend.map(_ * durationAsHours(durations.weekend))
    }

    lazy val transactionFee: Option[market.Money] = transactionFeeRate.map(total * _)

    /** The booking total minus the transaction fee. */
    def netTotal: market.Money = {
        total - transactionFee.getOrElse(moneyZero)
    }

    private def moneyZero: market.Money = {
        pricePerHour.currency(0)
    }

    private def durationAsHours(duration: Duration): BigDecimal = {
        BigDecimal(duration.getSeconds) / BigDecimal(3600.0)
    }
}
