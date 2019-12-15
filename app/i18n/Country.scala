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

package i18n

import squants.market

object Country extends Enumeration {

  /**
    * @param vat Local VAT tax, as a percentage.
    * @param states If the country has state or province subdivisions, maps
    * state codes (e.g. `NY`) to state names.
    */
  case class Val(
    val name: String, val currency: market.Currency, val isoCode: String,
    val accountType: AccountType.Val, val vat: Option[Double] = None,
    val addressFormat: AddressFormat.Value,
    val states: Map[String, String] = Map())
    extends super.Val

  /** Eurozone (EU-19) */
  val Austria = Val(
    "Austria", Currency.EUR, "AT", AccountType.Iban, Some(20),
    AddressFormat.European)
  val Belgium = Val("Belgium", Currency.EUR, "BE", AccountType.Iban, Some(21),
    AddressFormat.European)
  val Cyprus = Val("Cyprus", Currency.EUR, "CY", AccountType.Iban, Some(19),
    AddressFormat.European)
  val Estonia = Val("Estonia", Currency.EUR, "EE", AccountType.Iban, Some(20),
    AddressFormat.European)
  val Finland = Val("Finland", Currency.EUR, "FI", AccountType.Iban, Some(24),
    AddressFormat.European)
  val France = Val("France", Currency.EUR, "FR", AccountType.Iban, Some(20),
    AddressFormat.European)
  val Germany = Val("Germany", Currency.EUR, "DE", AccountType.Iban, Some(19),
    AddressFormat.European)
  val Greece = Val("Greece", Currency.EUR, "GR", AccountType.Iban, Some(24),
    AddressFormat.European)
  val Ireland = Val("Ireland", Currency.EUR, "IE", AccountType.Iban, Some(23),
    AddressFormat.European)
  val Italy = Val("Italy", Currency.EUR, "IT", AccountType.Iban, Some(22),
    AddressFormat.European)
  val Latvia = Val("Latvia", Currency.EUR, "LV", AccountType.Iban, Some(21),
    AddressFormat.European)
  val Lithuania = Val("Lithuania", Currency.EUR, "LT", AccountType.Iban,
    Some(21), AddressFormat.European)
  val Luxembourg = Val("Luxembourg", Currency.EUR, "LU", AccountType.Iban,
    Some(17), AddressFormat.European)
  val Malta = Val("Malta", Currency.EUR, "MT", AccountType.Iban, Some(18),
    AddressFormat.European)
  val Netherlands = Val("Netherlands", Currency.EUR, "NL", AccountType.Iban,
    Some(21), AddressFormat.European)
  val Portugal = Val("Portugal", Currency.EUR, "PT", AccountType.Iban,
    Some(23), AddressFormat.European)
  val Slovakia = Val("Slovakia", Currency.EUR, "SK", AccountType.Iban,
    Some(20), AddressFormat.European)
  val Slovenia = Val("Slovenia", Currency.EUR, "SI", AccountType.Iban,
    Some(22), AddressFormat.European)
  val Spain = Val("Spain", Currency.EUR, "ES", AccountType.Iban, Some(21),
    AddressFormat.European)

  /* Rest of EU */
  val Bulgaria = Val("Bulgaria", Currency.BGN, "BG", AccountType.Iban,
    Some(20), AddressFormat.European)
  val Denmark = Val("Denmark", Currency.DKK, "DK", AccountType.Iban,
    Some(25), AddressFormat.European)
  val Croatia = Val("Croatia", Currency.HRK, "HR", AccountType.Iban,
    Some(25), AddressFormat.European)
  val CzechRepublic = Val("Czech Republic", Currency.CZK, "CZ",
    AccountType.Iban, Some(21), AddressFormat.European)
  val Hungary = Val("Hungary", Currency.HUF, "HU", AccountType.Iban, Some(27),
    AddressFormat.Hungarian)
  val Poland = Val("Poland", Currency.PLN, "PL", AccountType.Iban, Some(23),
    AddressFormat.European)
  val Romania = Val("Romania", Currency.RON, "RO", AccountType.Iban,
    Some(19), AddressFormat.European)
  val Sweden = Val("Sweden", Currency.SEK, "SE", AccountType.Iban, Some(25),
    AddressFormat.European)
  val UnitedKingdom = Val("United Kingdom", Currency.GBP, "UK",
    AccountType.Iban, Some(20), AddressFormat.British)

  /* Europe, non EU */
  val Iceland = Val("Iceland", Currency.ISK, "IS", AccountType.Iban, Some(24),
    AddressFormat.European)
  val Norway = Val("Norway", Currency.NOK, "NO", AccountType.Iban, Some(25),
    AddressFormat.European)
  val Switzerland = Val("Switzerland", Currency.CHF , "CH", AccountType.Iban,
    Some(7.7), AddressFormat.European)

  /* Other */
  val Australia = Val("Australia", Currency.AUD, "AU",
    AccountType.AustralianAccount, Some(10), AddressFormat.English, Map(
      "ACT" -> "Australian Capital Territory",
      "NSW" -> "New South Wales",
      "NT" -> "Northern Territory",
      "QLD" -> "Queensland",
      "SA" -> "South Australia",
      "TAS" -> "Tasmania",
      "VIC" -> "Victoria",
      "WA" -> "Western Australia"))
  val Canada = Val("Canada", Currency.CAD, "CA", AccountType.CanadianAccount,
    None, AddressFormat.English, Map(
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
  val NewZealand = Val("New Zealand", Currency.NZD, "NZ",
    AccountType.NewZealandAccount, Some(15), AddressFormat.English)
  val UnitedStates = Val("United States", Currency.USD, "US",
    AccountType.AbaAccount, None, AddressFormat.American, Map(
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
