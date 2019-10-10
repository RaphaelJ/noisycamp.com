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
 *
 * Provides additional definitions over Squants market and currency manipulation
 * package.
 */

package i18n

import squants.market

object Currency {

  // Additional currencies not defined in Squants
  object BGN extends market.Currency("BGN", "Bulgarian lev", "лв.", 2)
  object HRK extends market.Currency("HRK", "Croatian kuna", "kn", 2)
  object HUF extends market.Currency("HUF", "Hungarian forint", "Ft", 2)
  object ISK extends market.Currency("ISK", "Icelandic króna", "kr", 0)
  object PLN extends market.Currency("PLN", "Polish złoty", "zł", 2)
  object RON extends market.Currency("RON", "Romanian leu", "L", 2)

  // Export aliases to existing Squants currencies
  def AUD = market.AUD
  def CAD = market.CAD
  def CHF = market.CHF
  def CZK = market.CZK
  def DKK = market.DKK
  def EUR = market.EUR
  def GBP = market.GBP
  def NOK = market.NOK
  def NZD = market.NZD
  def SEK = market.SEK
  def USD = market.USD

  val currencies: Set[market.Currency] = Set(
    // EU
    BGN, HRK, CZK, DKK, EUR, HUF, PLN, GBP, RON, SEK,

    // Europe, non EU
    NOK, CHF, ISK,

    // Other
    AUD, CAD, NZD, USD)

  def byCode: Map[String, market.Currency] = {
    currencies.map(c => c.code -> c).toMap
  }

  def moneyContext = market.MoneyContext(EUR, currencies, Seq.empty)
}
