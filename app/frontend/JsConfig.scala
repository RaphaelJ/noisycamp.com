/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2020  Raphael Javaux <raphael@noisycamp.com>
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

package frontend

import scala.concurrent.duration.Duration

import play.api.Configuration
import play.api.libs.json.{ JsNull, JsObject, Json, JsString, JsValue, Writes }
import play.api.mvc.RequestHeader
import views.html.helper.CSRF

import i18n.{ Country, Currency }
import misc.{ EquipmentCategory, EquipmentFamily }

/** Provides a JSON dump of the configuration variables used by the front-end
 * code. */
object JsConfig {

    def apply()(implicit config: Configuration, request: RequestHeader) : JsValue = {

        Json.obj(
            "csrfToken" -> CSRF.getToken.value,

            "currentUri" -> request.uri,

            "mapboxToken" -> config.get[String]("mapbox.token"),
            "stripePublicKey" -> config.get[String]("stripe.publicKey"),

            "cdnHost" -> config.getOptional[String]("noisycamp.cdnHost"),

            "bookingBeginsRoundingTime" -> 
                config.get[Duration]("noisycamp.bookingBeginsRoundingTime").toSeconds,
            "bookingDurationRoundingTime" -> 
                config.get[Duration]("noisycamp.bookingDurationRoundingTime").toSeconds,

            // Currencies

            "currencies" -> JsObject(
                for (curr <- Currency.currencies.toSeq)
                yield curr.code -> Json.obj(
                    "isoCode" -> curr.code,
                    "name" -> curr.name,
                    "symbol" -> curr.symbol,
                    "decimals" -> curr.formatDecimals,
                )
            ),

            // Lists the name, currency and provinces of every supported country.

            "countries" -> JsObject(
                for ((code, country) <- Country.byCode)
                yield code -> Json.obj(
                    "isoCode" -> code,
                    "name" -> country.name,
                    "currency" -> country.currency.code,
                    "states" -> {
                        if (country.states.nonEmpty) {
                            JsObject(
                                for ((stateCode, stateName) <- country.states)
                                yield stateCode -> JsString(stateName))
                        } else {
                            JsNull
                        }
                    },
                    "hasZipCode" -> country.hasZipCode
                )
            ),

            // Equipment and equipment families.

            "equipmentFamilies" -> JsObject(
                for ((code, family) <- EquipmentFamily.byCode)
                yield code -> Json.obj(
                    "code" -> family.code,
                    "name" -> family.name)
                ),

            "equipments" -> JsObject(
                for ((code, equipment) <- EquipmentCategory.byCode)
                yield code -> Json.obj(
                    "code" -> equipment.code,
                    "name" -> equipment.name,
                    "family" -> equipment.family.code)
                )
        )
    }
}
