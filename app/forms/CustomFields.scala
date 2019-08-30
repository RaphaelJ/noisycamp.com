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

import org.joda.time.{LocalTime}
import play.api.data.{ FormError, Mapping }
import play.api.data.format.Formatter
import play.api.data.Forms._

import misc.Country

object CustomFields {

  /** Accepts any valid country value (ISO code) from the `Country`
    * enumeration. */
  val country: Mapping[Country.Val] = {
    val countryFormat: Formatter[Country.Val] = new Formatter[Country.Val] {
      def bind(key: String, data: Map[String, String]) = {
        data.
          get(key).
          toRight(Seq(FormError(key, "error.required", Nil))).
          flatMap { isoCode =>
            Country.byCode.
              get(isoCode).
              toRight(Seq(FormError(key, "Not a valid country")))
          }
      }

      def unbind(key: String, value: Country.Val) = Map(key -> value.isoCode)
    }

    of[Country.Val](countryFormat)
  }

  /** Parses a `<input type="time">` as a JodaTime LocalTime value. */
  val jodaLocalTime: Mapping[LocalTime] = {
    val jodaLocalTimeFormat = new Formatter[LocalTime] {
      def bind(key: String, data: Map[String, String]) = {
        data.
          get(key).
          toRight(Seq(FormError(key, "error.required", Nil))).
          flatMap { timeStr =>
            try {
              Right(LocalTime.parse(timeStr))
            } catch {
              case _: IllegalArgumentException => {
                Left(Seq(FormError(key, "Invalid time format")))
              }
            }
          }
      }

      def unbind(key: String, value: LocalTime) = Map(key -> value.toString)
    }

    of[LocalTime](jodaLocalTimeFormat)
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
