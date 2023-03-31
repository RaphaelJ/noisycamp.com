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

import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant

import scala.language.implicitConversions
import scala.concurrent.Future

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.{ Format, FormatDetector }
import com.sksamuel.scrimage.nio.{ ImageWriter, GifWriter, JpegWriter, PngWriter }
import com.sksamuel.scrimage.webp.WebpWriter
import akka.compat.Future

import views.html.helper.form
import scala.util.control

case class PictureId(
    // SHA-256 hash of the content
    val value: Array[Byte]) {

    def base64 = java.util.Base64.getEncoder.encodeToString(value)
}

object PictureId {
    def fromString(str: String) = PictureId(java.util.Base64.getDecoder.decode(str))

    def fromContent(content: Array[Byte]) = {
        PictureId(MessageDigest.getInstance("SHA-256").digest(content))
    }
}

/** Parent class for all picture instances.
 *
 * By transparently supporting serialized and buffered image, we can avoid to unneedlessly read or
 * serialize images when it's not required.
 */
sealed abstract trait Picture {
    def createdAt:  Instant

    def image:      ImmutableImage
    def format:     PictureFormat
    def bytes:      Array[Byte]

    def serialized: SerializedPicture
    def buffered:   BufferedPicture
}

/** An not yet opened picture object (e.g. loaded from the database or from a file). */
final case class SerializedPicture(
    val id:         SerializedPicture#Id,
    val bytes:      Array[Byte],
    val format:     PictureFormat,
    val createdAt:  Instant = Instant.now(),
    ) extends Picture {

    type Id = PictureId

    def image: ImmutableImage = ImmutableImage.loader().fromBytes(bytes)

    def serialized = this
    def buffered: BufferedPicture = BufferedPicture(image, format, createdAt)
}

object SerializedPicture {
    /** Creates a new `SerializedPicture` object from a byte buffer by automatically detecting
     * the image's format. */
    def fromBytes(bytes: Array[Byte]): Option[SerializedPicture] = {
        val format = FormatDetector.detect(bytes)

        if (format.isPresent) {
            val pictureFormat = PictureFormat.fromScrimage(format.get())
            Some(SerializedPicture.fromBytes(bytes, pictureFormat))
        } else {
            None
        }
    }

    def fromBytes(bytes: Array[Byte], format: PictureFormat): SerializedPicture = {
        val pictureId = PictureId.fromContent(bytes)
        SerializedPicture(id=pictureId, bytes=bytes, format=format)
    }
}

/** A picture object that has been deserialized (e.g. decompressed from an image file). */
final case class BufferedPicture(
    val image:      ImmutableImage,
    val format:     PictureFormat,
    val createdAt:  Instant = Instant.now(),
    ) extends Picture {

    def bytes: Array[Byte] = image.bytes(format.writer)

    def serialized = SerializedPicture.fromBytes(bytes, format)
    def buffered = this
}

sealed trait PictureFormat {
    def isLossless:     Boolean
    def contentType:    String
    def writer:         ImageWriter
}

object PictureFormat {
    def fromScrimage(format: Format) = {
        format match {
            case Format.GIF => GifFormat
            case Format.JPEG => JpegFormat
            case Format.PNG => PngFormat
            case Format.WEBP => WebPFormat(isLossless = false)
        }
    }
}

final case object GifFormat extends PictureFormat {
    def isLossless = true
    def contentType = "image/gif"
    def writer = GifWriter.Default
}

final case object JpegFormat extends PictureFormat {
    def isLossless = false
    def contentType = "image/jpeg"
    def writer = JpegWriter.Default
}

final case object PngFormat extends PictureFormat {
    def isLossless = true
    def contentType = "image/png"
    def writer = new PngWriter()
}

final case class WebPFormat(val isLossless: Boolean) extends PictureFormat {
    def contentType = "image/webp"
    def writer: ImageWriter = {
        if (isLossless) {
            WebpWriter.DEFAULT.withLossless
        } else {
            WebpWriter.DEFAULT
        }
    }
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