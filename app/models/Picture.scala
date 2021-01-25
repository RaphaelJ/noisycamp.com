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

package models

import java.time.Instant
import scala.language.implicitConversions

import com.sksamuel.scrimage.format.Format

case class PictureId(
    // SHA-256 hash of the content
    val value: Array[Byte]) {

    def base64 = java.util.Base64.getEncoder.encodeToString(value)
}

object PictureId {
    def fromString(str: String) = PictureId(java.util.Base64.getDecoder.decode(str))
}

case class Picture(
    id:         Picture#Id,
    createdAt:  Instant     = Instant.now(),
    format:     Format,
    content:    Array[Byte]) {

    type Id = PictureId
}
