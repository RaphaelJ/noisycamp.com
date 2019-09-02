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

package misc

import java.time.Duration
import javax.inject.{ Inject, Singleton }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import play.api.libs.ws._
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

  def currenciesByCode: Map[String, market.Currency] = {
    currencies.map(c => c.code -> c).toMap
  }

  def moneyContext = market.MoneyContext(EUR, currencies, Seq.empty)
}

/** Periodically checks the ECB exchange rates server and provides up to date
 * exchange rates to the Euro. */
@Singleton
class ExchangeRateService @Inject() (
  actorSystem: ActorSystem,
  implicit val executionContext: ExecutionContext,
  ws: WSClient) {

  val updateInterval: FiniteDuration = 15.minutes

  updateExchangeRate // Initializes the service immediatly.

  actorSystem.scheduler.schedule(
    initialDelay = updateInterval, interval = updateInterval) {
    updateExchangeRate
  }

  /** Fetches and updates the exchange rates from the ECB website. */
  def updateExchangeRate(): Future[List[market.CurrencyExchangeRate]] = {
    val url = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    ws.url(url).
      withFollowRedirects(true).
      get.
      map { response =>
        val currByCode = Currency.currenciesByCode

        val nodes = response.xml \ "Cube" \ "Cube" \ "Cube"

        nodes.
          map { node =>
            val code: String = node.attribute("currency").get(0).text
            val rate: Double = node.attribute("rate").get(0).text.toDouble

            currByCode.
              get(code).
              map(curr => Currency.EUR / curr(rate))
          }.
          flatten.
          toList
      }
  }
}
