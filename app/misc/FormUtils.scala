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

package misc

import play.api.data.Form
import play.api.libs.json.{ JsObject, Json, JsString }

object FormUtils {
    /** Constructs a hierarchical JSON object from the form data. */
    def dataAsJson(form: Form[_]): JsObject = {
        /** Recursively goes through the key "." hierarchy (e.g.
         * "location.address.zipcode") and construct a single item JSON object.
         */
        def goValue(key: String, value: String): JsObject = {
            val (prefix, suffix) = key.span(_ != '.')

            val valueObj =
                if (suffix.isEmpty) {
                    JsString(value)
                } else {
                    goValue(suffix.drop(1), value)
                }

            if (prefix.last == ']') {
                // Node name contains a `[idx]` value.
                val idxStartsAt = prefix.lastIndexOf('[')
                val idx = prefix.drop(idxStartsAt + 1).dropRight(1)
                Json.obj(prefix.take(idxStartsAt) -> Json.obj(idx -> valueObj))
            } else {
                Json.obj(prefix -> valueObj)
            }
        }

        // Accumulates the data values into a JSON object hierarchy.
        form.data.
            foldLeft(Json.obj()){ case (obj, (key, value)) =>
                obj deepMerge goValue(key, value)
            }
    }
}
