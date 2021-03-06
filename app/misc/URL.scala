/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020 2021  Raphael Javaux <raphael@noisycamp.com>
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

import java.text.Normalizer

import play.api.Configuration
import play.api.mvc.{ Call, RequestHeader }

object URL {
    /** Returns the URL to a call from the CDN, or a relative path if there is no CDN.
     * 
     * @param absoluteURL if true, will return an absolute URL if there is no CDN.
     */
    def fromCDN(call: Call, absoluteURL: Boolean = false)(
        implicit req: RequestHeader, config: Configuration): String = {

        config.getOptional[String]("noisycamp.cdnHost") match {
            case Some(cdnHost) => cdnHost + call.canonical
            case None => {
                if (absoluteURL) {
                    call.absoluteURL
                } else {
                    call.canonical
                }
            }
        }
    }

    /** Generates a version of the title that can be used in URL. */ 
    def titleAsURL(title: String, max_length: Int = 60): String = {
        // Makes the title lower case and replaces spaces and non-letter/digit characters by '-'.
        val lowered = title.
            map { c => 
                if (c.isLetterOrDigit) { c.toLower }
                else { '-' }
            }    

        // Removes accents.
        val normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD).
            replaceAll("[^a-z0-9-]", "")

        normalized.take(max_length)
    }
}
