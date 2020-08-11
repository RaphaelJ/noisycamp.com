/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphael@noisycamp.com>
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

package forms.account


import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields

/** A form to request access to NoisyCamp Premium. */
object PremiumForm {

    val form = Form(
        mapping(
            "nonprofit"                 -> boolean,
            "website"                   -> CustomFields.optionalText,

            "rooms"                     -> number,

            "lower-transaction-rate"    -> boolean,
            "daily-payouts"             -> boolean,
            "calendar-sync"             -> boolean,
            "website-integration"       -> boolean,
            "manual-bookings"           -> boolean
        )(Data.apply)(Data.unapply))

    case class Data(
        nonprofit:              Boolean,
        website:                Option[String],

        rooms:                  Int,

        // Requested features
        lowerTransactionRate:   Boolean,
        dailyPayouts:           Boolean,
        calendarSync:           Boolean,
        websiteIntegration:     Boolean,
        manualBookings:         Boolean)
}
