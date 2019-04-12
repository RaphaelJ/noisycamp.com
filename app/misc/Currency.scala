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

package misc

object Currency extends Enumeration {

  case class Val(
    val name: String, val symbol: String, val isoCode: String)
    extends super.Val

  /* EU */
  val BulgarianLev = Val("Bulgarian lev", "лв.", "BGN")
  val CroatianKuna = Val("Croatian kuna", "kn", "HRK")
  val CzechKoruna = Val("Czech koruna", "Kč", "CZK")
  val DanishKrone = Val("Danish krone", "kr.", "DKK")
  val Euro = Val("Euro", "€", "EUR")
  val HungarianForint = Val("Hungarian forint", "Ft", "HUF")
  val PolishZloty = Val("Polish złoty", "zł", "PLN")
  val PoundSterling = Val("Pound sterling", "£", "GBP")
  val RomanianLeu = Val("Romanian leu", "RON", "RON")
  val SwedishKrona = Val("Swedish krona", "kr", "SEK")

  /* Other */
  val AustralianDollar = Val("Pound sterling", "AU$", "AUD")
  val CanadianDollar = Val("Canadian dollar", "CAD", "CAD")
  val NZDollar = Val("New Zealand dollar", "NZ$", "NZD")
  val USDollar = Val("U.S. dollar", "$", "USD")

  /** Maps currency ISO codes to `Currency` instances. */
  val byCode: Map[String, Val] = values.
    toList.
    map(_.asInstanceOf[Val]).
    map { v => v.isoCode -> v }.
    toMap
}
