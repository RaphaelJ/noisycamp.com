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
import play.api.i18n.MessagesProvider
import play.api.libs.json.{ JsObject, JsNull, Json, JsString, JsValue }
import play.api.data.Mapping
import play.api.data.FormError
import views.html.defaultpages.error

object FormUtils {
    /** Constructs a hierarchical JSON object from the form data. */
    def dataAsJson(form: Form[_]): JsObject = {
        // Accumulates the data values into a JSON object hierarchy.
        form.data.
            foldLeft(mappingAsJson(form.mapping)) { case (obj, (key, value)) =>
                obj deepMerge buildFromKey(key, JsString(value))
            }
    }

    /** Constructs a hierarchical JSON object from the form's errors.
     * Global field errors are exported as a special empty string field (e.g
     * `{ "": "Field required." }`). */
    def errorsAsJson(form: Form[_])(implicit messages: MessagesProvider): JsObject = {
        // Accumulates the error values into a JSON object hierarchy.

        /** Adds a new error object (built using `buildFromKey()`) to an existing error/mapping
         * tree.
         *
         * @param newObj shall be a single field object (possibly nested). */
        def mergeErrorObj(obj: JsObject, newObj: JsObject): JsObject = {
            require(newObj.value.size == 1)
            val (nestedKey, nestedNewValue) = newObj.value.head
            val nestedObjValue = obj.value.get(nestedKey)

            (nestedObjValue, nestedNewValue) match {
                case (Some(nestedObj@JsObject(_)), nestedNewObj@JsObject(_)) => {
                    // Merges the two existing error objects.
                    obj + (nestedKey -> mergeErrorObj(nestedObj, nestedNewObj))
                }
                case (Some(nestedObj@JsObject(_)), globalErrorMsg@JsString(_)) => {
                    // Adds a global error to a existing error object.
                    obj + (nestedKey -> (nestedObj + ("" -> globalErrorMsg)))
                }
                case (Some(globalErrorMsg@JsString(_)), nestedNewObj@JsObject(_)) => {
                    // Adds the already existing global error message to the new error object.
                    obj + (nestedKey -> (nestedNewObj + ("" -> globalErrorMsg)))
                }
                case _ => obj ++ newObj
            }
        }

        form.errors.
            foldLeft(mappingAsJson(form.mapping)) { case (obj, error) =>
                mergeErrorObj(obj, buildFromKey(error.key, JsString(error.format)))
            }
    }

    /** Constructs a hierarchical JSON object from the form fields, with a default JsNull values
     * (e.g. "{'location': { 'address': { 'zipcode': null, 'city': null } } }").
     */
    def mappingAsJson[_](mapping: Mapping[_]): JsObject = {
        mapping.mappings.
            // Ignore non-leaf mappings.
            filter(m => !m.key.isEmpty && m.mappings.filter(_ != m).isEmpty).
            map(_.key).
            foldLeft(Json.obj()) { case (obj, key) =>
                obj deepMerge buildFromKey(key, JsNull)
            }
    }

    // Recursively goes through the key "." hierarchy (e.g. "location.address.zipcode") and
    // construct a single item JSON object with the provided value (e.g.
    // "{'location': { 'address': { 'zipcode': value } } }").
    private def buildFromKey(key: String, value: JsValue): JsObject = {
        val (prefix, suffix) = key.span(_ != '.')

        val valueObj =
            if (suffix.isEmpty) {
                value
            } else {
                buildFromKey(suffix.drop(1), value)
            }

        if (!prefix.isBlank && prefix.last == ']') {
            // Node name contains a `[idx]` value.
            val idxStartsAt = prefix.lastIndexOf('[')
            val idx = prefix.drop(idxStartsAt + 1).dropRight(1)
            Json.obj(prefix.take(idxStartsAt) -> Json.obj(idx -> valueObj))
        } else {
            Json.obj(prefix -> valueObj)
        }
    }
}