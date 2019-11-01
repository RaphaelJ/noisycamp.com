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
 * Provides ways to fetch and manipulate currency exchange rates.
 */

package i18n

import java.util.concurrent.atomic.AtomicReference
import javax.inject.{ Inject, Singleton }

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.math.BigDecimal.RoundingMode

import akka.actor.ActorSystem
import org.joda.time.DateTime
import play.api.libs.concurrent.CustomExecutionContext
import play.api.libs.ws._
import squants.market

/** A set of exchange rates with their associated timestamp. */
case class ExchangeRates(rates: List[market.CurrencyExchangeRate],
  date: DateTime) {

  def context: market.MoneyContext = {
    Currency.moneyContext withExchangeRates rates
  }

  /** Converts the provided money amount to the target currency, rounding the
   * value to the nearest representable value (e.g. cents). */
  def exchange(src: market.Money, dst: market.Currency): market.Money = {
    val decimals = dst.formatDecimals
    src.in(dst)(context).rounded(decimals, RoundingMode.CEILING)
  }
}

/** Periodically checks the ECB exchange rates server and provides up to date
 * exchange rates to the Euro. */
@Singleton
class ExchangeRatesService @Inject() (
  actorSystem: ActorSystem,
  implicit val executionContext: ExecutionContext,
  ws: WSClient) {

  val updateInterval: FiniteDuration = 15.minutes

  def exchangeRates: ExchangeRates = exchangeRatesRef.get

  private val exchangeRatesRef: AtomicReference[ExchangeRates] =
    new AtomicReference(Await.result(updateExchangeRates, 1.minute))

  actorSystem.scheduler.schedule(
    initialDelay = updateInterval, interval = updateInterval) {
    updateExchangeRates.map(exchangeRatesRef.set _)
  }

  /** Fetches and updates the exchange rates from the ECB website. */
  private def updateExchangeRates(): Future[ExchangeRates] = {
    val url = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    ws.url(url).
      withFollowRedirects(true).
      get.
      map { response =>
        val date = DateTime.now()

        val nodes = response.xml \ "Cube" \ "Cube" \ "Cube"

        val rates: List[market.CurrencyExchangeRate] = nodes.
          map { node =>
            val code: String = node.attribute("currency").get(0).text
            val rate: Double = node.attribute("rate").get(0).text.toDouble

            Currency.byCode.
              get(code).
              map(curr => Currency.EUR / curr(rate))
          }.
          flatten.
          toList

        ExchangeRates(rates, date)
      }
  }
}
