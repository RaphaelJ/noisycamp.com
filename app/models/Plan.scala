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

import scala.language.implicitConversions

import squants.market

import i18n.Currency
import i18n.Currency._

object PayoutSchedule extends Enumeration {
    case class Val(val name: String) extends super.Val

    implicit def valueToVal(v: Value): Val = v.asInstanceOf[Val]

    val Daily = Val("Daily")
    val Weekly = Val("Weekly")
    val Monthly = Val("Monthly")
}

object Plan extends Enumeration {

    case class Val(
        val code:               String,
        val name:               String,

        val transactionRate:    BigDecimal,

        // Maximum number of studios per user. Unlimited if not defined.
        val studioLimit:        Option[Int],
        val payoutSchedule:     PayoutSchedule.Value,
        val calendarSync:       Boolean,
        val websiteIntegration: Boolean,
        val manualBookings:     Boolean,
        val onsitePayments:     Boolean,
        val equipmentFee:       Boolean,
        val socialAds:          Boolean,
        val smsReminders:       Boolean,

        val prices:             Option[Map[market.Currency, market.Money]],
        ) extends super.Val {

        require(
            prices.isEmpty || Currency.currencies.forall(prices.get.contains(_)),
            "Plan should contain a value for every currency in `i18n.Currency`.")
        require(
            prices.isEmpty || prices.get.forall { case (curr, money) => curr == money.currency },
            "Plan price should match its associated currency.")

        def isFree = prices.isEmpty
    }

    implicit def valueToVal(v: Value): Val = v.asInstanceOf[Val]

    val Free = Val(
        "free",
        "Free",
        transactionRate = BigDecimal(0.09),
        studioLimit = Some(1),
        payoutSchedule = PayoutSchedule.Monthly,
        calendarSync = false,
        websiteIntegration = false,
        manualBookings = false,
        onsitePayments = false,
        equipmentFee = false,
        socialAds = false,
        smsReminders = false,
        prices = None)

    val Standard = Val(
        "standard",
        "Standard",
        transactionRate = BigDecimal(0.06),
        studioLimit = Some(4),
        payoutSchedule = PayoutSchedule.Weekly,
        calendarSync = true,
        websiteIntegration = true,
        manualBookings = true,
        onsitePayments = true,
        equipmentFee = true,
        socialAds = false,
        smsReminders = false,
        prices = Some(Map(
            USD -> USD(25),
            EUR -> EUR(19),

            BGN -> BGN(39),
            HRK -> HRK(139),
            CZK -> CZK(499),
            DKK -> DKK(149),
            HUF -> HUF(6900),
            PLN -> PLN(89),
            GBP -> GBP(16),
            RON -> RON(99),
            SEK -> SEK(199),
            NOK -> NOK(199),
            CHF -> CHF(19),
            ISK -> ISK(2900),
            AUD -> AUD(29),
            CAD -> CAD(29),
            HKD -> HKD(169),
            NZD -> NZD(29),
            SGD -> SGD(29),
        )))

    val Premium = Val(
        "premium",
        "Premium",
        transactionRate = BigDecimal(0.04),
        studioLimit = None,
        payoutSchedule = PayoutSchedule.Daily,
        calendarSync = true,
        websiteIntegration = true,
        manualBookings = true,
        onsitePayments = true,
        equipmentFee = true,
        socialAds = true,
        smsReminders = true,
        prices = Some(Map(
            USD -> USD(79),
            EUR -> EUR(69),

            BGN -> BGN(129),
            HRK -> HRK(499),
            CZK -> CZK(1600),
            DKK -> DKK(490),
            HUF -> HUF(25000),
            PLN -> PLN(299),
            GBP -> GBP(59),
            RON -> RON(339),
            SEK -> SEK(699),
            NOK -> NOK(699),
            CHF -> CHF(69),
            ISK -> ISK(10000),
            AUD -> AUD(99),
            CAD -> CAD(99),
            HKD -> HKD(599),
            NZD -> NZD(115),
            SGD -> SGD(99),
        )))

    lazy val byCode = values.map(p => p.code -> p).toMap

    def default = Free

    require(default.isFree)
}
