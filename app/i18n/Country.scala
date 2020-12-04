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
     * @param namePrefix A prefix that should be added to the country name, like in
     * "The Netherlands".
     */
    case class Val(
        val name: String, val currency: market.Currency, val isoCode: String,
        val vat: Option[Double] = None, val addressFormat: AddressFormat.Value,
        val states: Map[String, String] = Map(), val hasZipCode: Boolean = true,
        val namePrefix: Option[String] = None)
        extends super.Val

    /** Eurozone (EU-19) */
    val Austria = Val("Austria", Currency.EUR, "AT", Some(20), AddressFormat.European)
    val Belgium = Val("Belgium", Currency.EUR, "BE", Some(21), AddressFormat.European)
    val Cyprus = Val("Cyprus", Currency.EUR, "CY", Some(19), AddressFormat.European)
    val Estonia = Val("Estonia", Currency.EUR, "EE", Some(20), AddressFormat.European)
    val Finland = Val("Finland", Currency.EUR, "FI", Some(24), AddressFormat.European)
    val France = Val("France", Currency.EUR, "FR", Some(20), AddressFormat.European)
    val Germany = Val("Germany", Currency.EUR, "DE", Some(19), AddressFormat.European)
    val Greece = Val("Greece", Currency.EUR, "GR", Some(24), AddressFormat.European)
    val Ireland = Val("Ireland", Currency.EUR, "IE", Some(23), AddressFormat.European)
    val Italy = Val("Italy", Currency.EUR, "IT", Some(22), AddressFormat.European)
    val Latvia = Val("Latvia", Currency.EUR, "LV", Some(21), AddressFormat.European)
    val Lithuania = Val("Lithuania", Currency.EUR, "LT", Some(21), AddressFormat.European)
    val Luxembourg = Val("Luxembourg", Currency.EUR, "LU", Some(17), AddressFormat.European)
    val Malta = Val("Malta", Currency.EUR, "MT", Some(18), AddressFormat.European)
    val Netherlands = Val("Netherlands", Currency.EUR, "NL", Some(21), AddressFormat.European,
        namePrefix = Some("The"))
    val Portugal = Val("Portugal", Currency.EUR, "PT", Some(23), AddressFormat.European)
    val Slovakia = Val("Slovakia", Currency.EUR, "SK", Some(20), AddressFormat.European)
    val Slovenia = Val("Slovenia", Currency.EUR, "SI", Some(22), AddressFormat.European)
    val Spain = Val("Spain", Currency.EUR, "ES", Some(21), AddressFormat.European)

    /* Rest of EU */
    //val Bulgaria = Val("Bulgaria", Currency.BGN, "BG", Some(20), AddressFormat.European)
    // val Denmark = Val("Denmark", Currency.DKK, "DK", Some(25), AddressFormat.European)
    // val Croatia = Val("Croatia", Currency.HRK, "HR", Some(25), AddressFormat.European)
    // val CzechRepublic = Val("Czech Republic", Currency.CZK, "CZ", Some(21),
    //    AddressFormat.European, namePrefix = Some("The"))
    // val Hungary = Val("Hungary", Currency.HUF, "HU", Some(27), AddressFormat.Hungarian)
    val Poland = Val("Poland", Currency.PLN, "PL", Some(23), AddressFormat.European)
    val Romania = Val("Romania", Currency.RON, "RO", Some(19), AddressFormat.European)
    // val Sweden = Val("Sweden", Currency.SEK, "SE", Some(25), AddressFormat.European)
  
    /* Europe, non EU */
    // val Iceland = Val("Iceland", Currency.ISK, "IS", Some(24), AddressFormat.European)
    // val Norway = Val("Norway", Currency.NOK, "NO", Some(25), AddressFormat.European)
    // val Switzerland = Val("Switzerland", Currency.CHF , "CH", Some(7.7), AddressFormat.European)
    val UnitedKingdom = Val("United Kingdom", Currency.GBP, "UK", Some(20), AddressFormat.British,
        namePrefix = Some("The"))

    /* Other */
    val Australia = Val("Australia", Currency.AUD, "AU", Some(10),
        AddressFormat.English, Map(
            "ACT" -> "Australian Capital Territory",
            "NSW" -> "New South Wales",
            "NT" -> "Northern Territory",
            "QLD" -> "Queensland",
            "SA" -> "South Australia",
            "TAS" -> "Tasmania",
            "VIC" -> "Victoria",
            "WA" -> "Western Australia"))
    // val Canada = Val("Canada", Currency.CAD, "CA", None, AddressFormat.English,
    //     Map(
    //         "AB" -> "Alberta",
    //         "BC" -> "British Columbia",
    //         "MB" -> "Manitoba",
    //         "NB" -> "New Brunswick",
    //         "NL" -> "Newfoundland and Labrador",
    //         "NT" -> "Northwest Territories",
    //         "NS" -> "Nova Scotia",
    //         "NU" -> "Nunavut",
    //         "ON" -> "Ontario",
    //         "PE" -> "Prince Edward Island",
    //         "QC" -> "Québec",
    //         "SK" -> "Saskatchewan",
    //         "YT" -> "Yulon"))
    // val HongKong = Val("Hong Kong", Currency.HKD, "HK", None, AddressFormat.HongKong,
    //     hasZipCode = false)
    val NewZealand = Val("New Zealand", Currency.NZD, "NZ", Some(15), AddressFormat.English)
    val Singapore = Val("Singapore", Currency.SGD, "SG", Some(7), AddressFormat.English)
    val UnitedStates = Val("United States", Currency.USD, "US", None, AddressFormat.American, 
        Map(
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
            "VI" -> "Virgin Islands"), namePrefix = Some("The"))

    /** Maps country ISO codes to `Country` instances. */
    val byCode: Map[String, Val] = values.
        toSeq.
        map(_.asInstanceOf[Val]).
        map { v => v.isoCode -> v }.
        toMap
}
