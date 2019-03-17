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

import play.api.data.FormError

import misc.Country

object CustomFields {

  /** Accepts any valid country value (ISO code) from the `Country`
    * enumeration. */
  val country: Mapping[Country] = {
    val countryFormat: Formatter[Country] = new Formatter[Country] {
      def bind(key: String, data: Map[String, String]) = {
        data.
          get(key).
          toRight(Seq(FormError(key, "error.required", Nil))).
          flatMap { isoCode =>
            Country.byCode.
              get(isoCode).
              toRight(Seq(FormError(key, "Not a valid country"))
          }
      }

      def unbind(key: String, value: Country) = Map(key -> value.isoCode)
    }

    of[Country](countryFormat)
  }
}
