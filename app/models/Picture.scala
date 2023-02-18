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

import java.net.URI
import java.nio.file.Path
import java.time.Instant

import scala.language.implicitConversions
import scala.concurrent.Future

import com.sksamuel.scrimage.format.Format
import akka.compat.Future
import java.io.InputStream

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
    format:     PictureFormat,
    content:    Array[Byte]) {

    type Id = PictureId
}

sealed trait PictureFormat {
    def isLossless: Boolean

    def contentType: String
}

final case object JpegFormat extends PictureFormat {
    def isLossless = false
    def contentType = "image/jpeg"
}

final case object GifFormat extends PictureFormat {
    def isLossless = true
    def contentType = "image/gif"
}

final case object PngFormat extends PictureFormat {
    def isLossless = true
    def contentType = "image/png"
}

final case class WebPFormat(val isLossless: Boolean) extends PictureFormat {
    def contentType = "image/webp"
}

sealed trait PictureSource {
    def cacheKey: String
}

final case class PictureFromFile(
    val path: Path,
    ) extends PictureSource {
    def cacheKey = "PictureFromFile(%s)".format(path.toString)
}

final case class PictureFromDatabase(
    val id: PictureId
    ) extends PictureSource {
    def cacheKey: String = "PictureFromDatabase(%s)".format(id.base64)
}

final case class PictureFromStream(
    val uri: URI,
    val stream: InputStream
    ) extends PictureSource {
    def cacheKey: String = "PictureFromStream(%s)".format(uri.toString)
}

final case class PictureFromUrl(
    val url: String
    ) extends PictureSource {
    def cacheKey: String = "PictureFromUrl(%s)".format(url)
}