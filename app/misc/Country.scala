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

object Country extends Enumeration {

  /** @param vat Local VAT tax, at a percentage. */
  protected case class Val(
    name: String, currency: Currency, isoCode: String,
    vat: Option[Double] = None)
    extends super.Val

  /** Eurozone (EU-19) */
  val Austria = Val("Austria", Euro, "AT", Some(20))
  val Belgium = Val("Belgium", Euro, "BE", Some(21))
  val Cyprus = Val("Cyprus", Euro, "CY", Some(19))
  val Estonia = Val("Estonia", Euro, "EE", Some(20))
  val Finland = Val("Finland", Euro, "FI", Some(24))
  val France = Val("France", Euro, "FR", Some(20))
  val Germany = Val("Germany", Euro, "DE", Some(19))
  val Greece = Val("Greece", Euro, "GR", Some(24))
  val Ireland = Val("Ireland", Euro, "IE", Some(23))
  val Italy = Val("Italy", Euro, "IT", Some(22))
  val Latvia = Val("Latvia", Euro, "LV", Some(21))
  val Lithuania = Val("Lithuania", Euro, "LT", Some(21))
  val Luxembourg = Val("Luxembourg", Euro, "LU", Some(17))
  val Malta = Val("Malta", Euro, "MT", Some(18))
  val Netherlands = Val("Netherlands", Euro, "NL", Some(21))
  val Portugal = Val("Portugal", Euro, "PT", Some(23))
  val Slovakia = Val("Slovakia", Euro, "SK", Some(20))
  val Slovenia = Val("Slovenia", Euro, "SI", Some(22))
  val Spain = Val("Spain", Euro, "ES", Some(21))

  /* Rest of EU */
  val Bulgaria = Val("Bulgaria", BulgarianLev, "BG", Some(20))
  val Denmark = Val("Denmark", DanishKrone, "DK", Some(25))
  val Croatia = Val("Croatia", CroatianKuna, "HR", Some(25))
  val CzechRepublic = Val("Czech Republic", CzechKoruna, "CZ", Some(21))
  val Hungary = Val("Hungary", HungarianForint, "HU", Some(27))
  val Poland = Val("Poland", PolishZloty, "PL", Some(23))
  val Romania = Val("Romania", RomanianLeu, "RO", Some(19))
  val Sweden = Val("Sweden", SwedishKrona, "SE", Some(25))
  val UnitedKingdom = Val("United Kingdom", PoundSterling, "UK", Some(20))

  /* Other */
  // val Australia = Val("Australia", AustralianDollar, "AU")
  val Canada = Val("Canada", CanadianDollar, "CA")
  // val NewZealand = Val("New Zealand", NZDollar, "NZ")
  val UnitedStates = Val("United States", USDollar, "US")

  /** Maps country ISO codes to `Country` instances. */
  val byCode: Map[String, Val] = Map(values.map { v.isoCode -> v })
}
