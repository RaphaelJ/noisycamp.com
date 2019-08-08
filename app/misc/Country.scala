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

  case class State(val name: String, val code: String)

  /**
    * @param vat Local VAT tax, as a percentage.
    * @param states If the country has state or province subdivisions, maps
    * state codes (e.g. `NY`) to state names.
    */
  case class Val(
    val name: String, val currency: Currency.Val, val isoCode: String,
    val payoutMethod: PayoutMethod.Val, val vat: Option[Double] = None,
    val states: Map[String, String] = Map())
    extends super.Val

  /** Eurozone (EU-19) */
  val Austria = Val("Austria", Currency.Euro, "AT", PayoutMethod.Iban, Some(20))
  val Belgium = Val("Belgium", Currency.Euro, "BE", PayoutMethod.Iban, Some(21))
  val Cyprus = Val("Cyprus", Currency.Euro, "CY", PayoutMethod.Iban, Some(19))
  val Estonia = Val("Estonia", Currency.Euro, "EE", PayoutMethod.Iban, Some(20))
  val Finland = Val("Finland", Currency.Euro, "FI", PayoutMethod.Iban, Some(24))
  val France = Val("France", Currency.Euro, "FR", PayoutMethod.Iban, Some(20))
  val Germany = Val("Germany", Currency.Euro, "DE", PayoutMethod.Iban, Some(19))
  val Greece = Val("Greece", Currency.Euro, "GR", PayoutMethod.Iban, Some(24))
  val Ireland = Val("Ireland", Currency.Euro, "IE", PayoutMethod.Iban, Some(23))
  val Italy = Val("Italy", Currency.Euro, "IT", PayoutMethod.Iban, Some(22))
  val Latvia = Val("Latvia", Currency.Euro, "LV", PayoutMethod.Iban, Some(21))
  val Lithuania = Val("Lithuania", Currency.Euro, "LT", PayoutMethod.Iban,
    Some(21))
  val Luxembourg = Val("Luxembourg", Currency.Euro, "LU", PayoutMethod.Iban,
    Some(17))
  val Malta = Val("Malta", Currency.Euro, "MT", PayoutMethod.Iban, Some(18))
  val Netherlands = Val("Netherlands", Currency.Euro, "NL", PayoutMethod.Iban,
    Some(21))
  val Portugal = Val("Portugal", Currency.Euro, "PT", PayoutMethod.Iban,
    Some(23))
  val Slovakia = Val("Slovakia", Currency.Euro, "SK", PayoutMethod.Iban,
    Some(20))
  val Slovenia = Val("Slovenia", Currency.Euro, "SI", PayoutMethod.Iban,
    Some(22))
  val Spain = Val("Spain", Currency.Euro, "ES", PayoutMethod.Iban, Some(21))

  /* Rest of EU */
  val Bulgaria = Val("Bulgaria", Currency.BulgarianLev, "BG", PayoutMethod.Iban,
    Some(20))
  val Denmark = Val("Denmark", Currency.DanishKrone, "DK", PayoutMethod.Iban,
    Some(25))
  val Croatia = Val("Croatia", Currency.CroatianKuna, "HR", PayoutMethod.Iban,
    Some(25))
  val CzechRepublic =
      Val("Czech Republic", Currency.CzechKoruna, "CZ", PayoutMethod.Iban,
        Some(21))
  val Hungary = Val("Hungary", Currency.HungarianForint, "HU",
    PayoutMethod.Iban, Some(27))
  val Poland = Val("Poland", Currency.PolishZloty, "PL", PayoutMethod.Iban,
    Some(23))
  val Romania = Val("Romania", Currency.RomanianLeu, "RO", PayoutMethod.Iban,
    Some(19))
  val Sweden = Val("Sweden", Currency.SwedishKrona, "SE", PayoutMethod.Iban,
    Some(25))
  val UnitedKingdom = Val(
    "United Kingdom", Currency.PoundSterling, "UK", PayoutMethod.Iban, Some(20))

  /* Europe, non EU */
  val Norway = Val("Norway", Currency.NorwegianKrone, "NO", PayoutMethod.Iban,
    Some(25))
  val Switzerland = Val("Switzerland", Currency.SwissFranc , "CH",
    PayoutMethod.Iban, Some(25))

  /* Other */
  val Australia = Val("Australia", Currency.AustralianDollar, "AU",
    PayoutMethod.Australian, Some(10), Map(
      "ACT" -> "Australian Capital Territory",
      "NSW" -> "New South Wales",
      "NT" -> "Northern Territory",
      "QLD" -> "Queensland",
      "SA" -> "South Australia",
      "TAS" -> "Tasmania",
      "VIC" -> "Victoria",
      "WA" -> "Western Australia"))
  val Canada = Val("Canada", Currency.CanadianDollar, "CA",
    PayoutMethod.Canadian, None, Map(
      "AB" -> "Alberta",
      "BC" -> "British Columbia",
      "MB" -> "Manitoba",
      "NB" -> "New Brunswick",
      "NL" -> "Newfoundland and Labrador",
      "NT" -> "Northwest Territories",
      "NS" -> "Nova Scotia",
      "NU" -> "Nunavut",
      "ON" -> "Ontario",
      "PE" -> "Prince Edward Island",
      "QC" -> "Québec",
      "SK" -> "Saskatchewan",
      "YT" -> "Yulon"))
  val NewZealand = Val("New Zealand", Currency.NZDollar, "NZ",
    PayoutMethod.NewZealand, Some(15))
  val UnitedStates = Val("United States", Currency.USDollar, "US",
    PayoutMethod.Aba, None, Map(
      /* States */
      "AL" -> "Alabama",
      "AK" -> "Alaska",
      "AZ" -> "Arizona",
      "AR" -> "Arkansas",
      "CA" -> "California",
      "CO" -> "Colorado",
      "CT" -> "Connecticut",
      "DE" -> "Delaware",
      "DC" -> "District of Columbia",
      "FL" -> "Florida",
      "GA" -> "Georgia",
      "HI" -> "Hawaii",
      "ID" -> "Idaho",
      "IL" -> "Illinois",
      "IN" -> "Indiana",
      "IA" -> "Iowa",
      "KS" -> "Kansas",
      "KY" -> "Kentucky",
      "LA" -> "Louisiana",
      "ME" -> "Maine",
      "MD" -> "Maryland",
      "MA" -> "Massachusetts",
      "MI" -> "Michigan",
      "MN" -> "Minnesota",
      "MS" -> "Mississippi",
      "MO" -> "Missouri",
      "MT" -> "Montana",
      "NE" -> "Nebraska",
      "NV" -> "Nevada",
      "NH" -> "New Hampshire",
      "NJ" -> "New Jersey",
      "NM" -> "New Mexico",
      "NY" -> "New York",
      "NC" -> "North Carolina",
      "ND" -> "North Dakota",
      "OH" -> "Ohio",
      "OK" -> "Oklahoma",
      "OR" -> "Oregon",
      "PA" -> "Pennsylvania",
      "RI" -> "Rhode Island",
      "SC" -> "South Carolina",
      "SD" -> "South Dakota",
      "TN" -> "Tennessee",
      "TX" -> "Texas",
      "UT" -> "Utah",
      "VT" -> "Vermont",
      "VA" -> "Virginia",
      "WA" -> "Washington",
      "WV" -> "West Virginia",
      "WI" -> "Wisconsin",
      "WY" -> "Wyoming",

      /* Territories */
      "AS" -> "American Samoa",
      "GU" -> "Guam",
      "MP" -> "Northern Mariana Islands",
      "PR" -> "Puerto Rico",
      "VI" -> "Virgin Islands"))

  /** Maps country ISO codes to `Country` instances. */
  val byCode: Map[String, Val] = values.
    toSeq.
    map(_.asInstanceOf[Val]).
    map { v => v.isoCode -> v }.
    toMap
}