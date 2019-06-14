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

package misc

import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

import com.github.benmanes.caffeine.cache.{ Caffeine, Weigher }
import play.api.Configuration
import scalacache._
import scalacache.caffeine._
import scalacache.modes.scalaFuture._

import models.Picture

case class PictureCacheKey(
  id: Array[Byte], maxWidth: Option[Int], maxHeight: Option[Int])

/** Cache uploaded pictures resizing operations between HTTP requests. */
@Singleton
class PictureCache @Inject() (val config: Configuration)
  (implicit executionContext: ExecutionContext) {

  def get(picture: Picture, maxWidth: Option[Int], maxHeight: Option[Int])
    : Future[Picture] = {

    if (maxWidth.isEmpty && maxHeight.isEmpty) {
      // TODO: does not process the image if maxWidth/Height > width/height
      Future.successful(picture)
    } else {
      val key = PictureCacheKey(picture.id, maxWidth, maxHeight).toString

      cachingF(key)(ttl = None){
        Future { PictureUtils.resize(picture, maxWidth, maxHeight) }
      }
    }
  }

  private implicit val _cache: Cache[Picture] = {
    val maxCacheSize = config.underlying.
      getBytes("noisycamp.picturesMaxCacheSize")
    
    val underlying = Caffeine.newBuilder().
      maximumWeight(maxCacheSize).
      weigher(new Weigher[String, Entry[Picture]] {
        def weigh(key: String, picture: Entry[Picture]) = {
          picture.value.content.length
        }
      }).
      build[String, Entry[Picture]]

    CaffeineCache(underlying)
  }
}
