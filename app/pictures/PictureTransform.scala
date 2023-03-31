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

import models.{ BufferedPicture, JpegFormat, GifFormat, PngFormat, Picture, PictureFormat,
    WebPFormat }
import views.html.helper.form

/** A descriptor of an operation that is being applyied on an image.
 *
 * Compared to a function, a transform can be serialized, and cached by the
 * `PictureCache`.
 * */
sealed trait PictureTransform extends Function[Picture, Picture] {
    def apply(picture: Picture): Picture
    def cacheKey: String
}

/** Applies the provided transformations sequentially. */
case class ChainedTransforms(val transforms: Seq[PictureTransform])
    extends PictureTransform {

    def apply(picture: Picture): Picture = {
        transforms.foldLeft(picture)((pic, transform) => transform(pic))
    }

    def cacheKey = "ChainedTransforms(%s)".format(transforms.map(_.cacheKey).mkString(","))
}

sealed trait ScrimageTransform extends PictureTransform {
    def transform(image: ImmutableImage): ImmutableImage

    def apply(picture: Picture): Picture = {
        BufferedPicture(image=transform(picture.image), format=picture.format)
    }
}

/** See Scrimage's `Image.bound()`. */
case class BoundPicture(val width: Int, val height: Int)
    extends ScrimageTransform {

    def transform(image: ImmutableImage): ImmutableImage = image.bound(width, height)

    def cacheKey = "BoundPicture(%d, %d)".format(width, height)
}

/** See Scrimage's `Image.cover()`. */
case class CoverPicture(val width: Int, val height: Int)
    extends ScrimageTransform {

    def transform(image: ImmutableImage): ImmutableImage = image.cover(width, height)

    def cacheKey = "CoverPicture(%d, %d)".format(width, height)
}

/** See Scrimage's `Image.max()`. */
case class MaxPicture(val width: Int, val height: Int)
    extends ScrimageTransform {

    def transform(image: ImmutableImage): ImmutableImage = image.max(width, height)

    def cacheKey = "CoverPicture(%d, %d)".format(width, height)
}

/** Changes the picture format to the requested one. */
case class ConvertFormat(val newFormat: PictureFormat)
    extends PictureTransform {

    def apply(picture: Picture) = {
        if (picture.format == newFormat) {
            picture
        } else {
            BufferedPicture(image=picture.image, format=newFormat)
        }
    }

    def cacheKey = "ConvertFormat(%s)".format(newFormat)
}

/** Converts the picture to a better image format when possible. */
case object OptimizeFormat extends PictureTransform {

    def apply(picture: Picture) = {
        val newFormat = picture.format match {
            case JpegFormat => WebPFormat(isLossless = false)
            case PngFormat => WebPFormat(isLossless = true)
            case _ => picture.format
        }
        ConvertFormat(newFormat)(picture)
    }

    def cacheKey = "OptimizeFormat"
}

/** Converts the picture to a legacy image format if required. */
case object LegacyFormat extends PictureTransform {

    def apply(picture: Picture) = {
        val newFormat = picture.format match {
            case WebPFormat(true) => PngFormat
            case WebPFormat(false) => JpegFormat
            case _ => picture.format
        }
        ConvertFormat(newFormat)(picture)
    }

    def cacheKey = "LegacyFormat"
}

