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

package pictures

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.{ Format, FormatDetector }
import com.sksamuel.scrimage.nio.{ GifWriter, ImageWriter, JpegWriter, PngWriter }
import com.sksamuel.scrimage.webp.WebpWriter

import models.{ JpegFormat, GifFormat, PngFormat, Picture, PictureFormat, WebPFormat }
import views.html.helper.form

/** A descriptor of an operation that is being applyied on an image.
 *
 * Compared to a function, a transform can be serialized, and cached by the
 * `PictureCache`.
 * */
sealed trait PictureTransform {
    def apply(pic: Picture): Picture
}

/** Does not apply any transformation on the picture. */
case object RawPicture extends PictureTransform {
    def apply(pic: Picture) = pic
}

/** See Scrimage's `Image.bound()`. */
case class BoundPicture(val width: Int, val height: Int)
    extends PictureTransform {

    def apply(pic: Picture) = {
        PictureTransform.transform(pic, { _.bound(width, height) })
    }
}

/** See Scrimage's `Image.cover()`. */
case class CoverPicture(val width: Int, val height: Int)
    extends PictureTransform {

    def apply(pic: Picture) = {
        PictureTransform.transform(pic, { _.cover(width, height) })
    }
}

/** See Scrimage's `Image.max()`. */
case class MaxPicture(val width: Int, val height: Int)
    extends PictureTransform {

    def apply(pic: Picture) = {
        PictureTransform.transform(pic, { _.max(width, height) })
    }
}

object PictureTransform {

    /** Applies the given Scrimage transformation function on the given `Picture`
     * object, keeping the same image format. */
    def transform(picture: Picture, func: (ImmutableImage => ImmutableImage)): Picture = {
        val img = ImmutableImage.loader().fromBytes(picture.content)
        val newImg = func(img)

        if (newImg != img) {
            val writer = formatWriter(picture.format)
            picture.copy(content=newImg.bytes(writer))
        } else {
            picture
        }
    }

    /** Changes the picture format to the requested one. */
    def convertFormat(picture: Picture, newFormat: PictureFormat): Picture = {
        if (picture.format == newFormat) {
            picture
        } else {
            val img = ImmutableImage.loader().fromBytes(picture.content)

            val writer = formatWriter(newFormat)
            picture.copy(content=img.bytes(writer), format=newFormat)
        }
    }

    /** Converts the picture to a better image format when possible. */
    def optimiseFormat(picture: Picture): Picture = {
        val newFormat = picture.format match {
            case JpegFormat => WebPFormat(isLossless = false)
            case PngFormat => WebPFormat(isLossless = true)
            case _ => picture.format
        }
        convertFormat(picture, newFormat)
    }

    protected def formatWriter(format: PictureFormat): ImageWriter = {
        format match {
            case GifFormat => GifWriter.Default
            case JpegFormat => JpegWriter.Default
            case PngFormat => new PngWriter()
            case WebPFormat(isLossless) => {
                if (isLossless) {
                    WebpWriter.DEFAULT.withLossless
                } else {
                    WebpWriter.DEFAULT
                }
            }
        }
    }
}
