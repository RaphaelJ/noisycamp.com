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

package models

object PayoutSchedule extends Enumeration {
    val Daily = Value
    val Weekly = Value
    val Monthly = Value
}

object Plan extends Enumeration {

    case class Val(
        val name:               String,

        val transactionRate:    BigDecimal,
        // Maximum number of studios per user. Unlimited if not defined.
        val studioLimit:        Option[Int],
        val payoutSchedule:     PayoutSchedule.Value,
        val calendarSync:       Boolean,
        val websiteIntegration: Boolean,
        val manualBookings:     Boolean,
        val equipmentFee:       Boolean)
        extends super.Val

    val Free = Val(
        "Free",
        transactionRate = BigDecimal(0.09),
        studioLimit = Some(1),
        payoutSchedule = PayoutSchedule.Monthly,
        calendarSync = false,
        websiteIntegration = false,
        manualBookings = false,
        equipmentFee = false)

    val Premium = Val(
        "Premium",
        transactionRate = BigDecimal(0.06),
        studioLimit = None,
        payoutSchedule = PayoutSchedule.Daily,
        calendarSync = true,
        websiteIntegration = true,
        manualBookings = true,
        equipmentFee = true)
}
