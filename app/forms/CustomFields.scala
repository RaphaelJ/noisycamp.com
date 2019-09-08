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
 */

package forms

import org.joda.time.LocalTime
import play.api.data.{ FormError, Mapping }
import play.api.data.format.Formatter
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import squants.market

import misc.{ Country, Currency }

object CustomFields {

  /** Accepts any valid country value (ISO code) from the `Country`
    * enumeration. */
  val country: Mapping[Country.Val] = {
    val countryFormat: Formatter[Country.Val] = new Formatter[Country.Val] {
      def bind(key: String, data: Map[String, String]) = {
        parsing(Country.byCode(_), "Not a valid country", Nil)(key, data)
      }

      def unbind(key: String, value: Country.Val) = Map(key -> value.isoCode)
    }

    of[Country.Val](countryFormat)
  }

  /** Maps a currency code to a `Currency` value. */
  val currency: Mapping[market.Currency] = {
    val currencyFormat = new Formatter[market.Currency] {
      def bind(key: String, data: Map[String, String]) = {
        parsing(Currency.currenciesByCode(_), "Invalid currency", Nil)(
          key, data)
      }

      def unbind(key: String, value: market.Currency) = Map(key -> value.code)
    }

    of(currencyFormat)
  }

  /** Parses a `<input type="time">` as a JodaTime LocalTime value. */
  val jodaLocalTime: Mapping[LocalTime] = {
    val jodaLocalTimeFormat = new Formatter[LocalTime] {
      def bind(key: String, data: Map[String, String]) = {
        parsing(LocalTime.parse, "Invalid time format", Nil)(key, data)
      }

      def unbind(key: String, value: LocalTime) = Map(key -> value.toString)
    }

    of[LocalTime](jodaLocalTimeFormat)
  }

  /** Parses a number field as a money amount. */
  val money: Mapping[BigDecimal] = {
    bigDecimal.
      verifying(min(BigDecimal(0.0)))
  }

  /** Similar to `text`, but will bind empty string to a `None` value. */
  val optionalText: Mapping[Option[String]] = {
    text.transform(
      str => str match {
        case "" => None
        case _ => Some(str)
      },
      _.getOrElse("")
    )
  }
}
