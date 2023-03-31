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

import java.nio.file.Path
import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

import com.github.benmanes.caffeine.cache.{ Caffeine, Weigher }
import play.api.Configuration
import scalacache._
import scalacache.caffeine._
import scalacache.modes.scalaFuture._

import misc.TaskExecutionContext
import models.{ PictureSource, SerializedPicture }
import views.html.emails.tags.a

case class PictureCacheKey(
    source:     PictureSource,
    transform:  Option[PictureTransform]) {

    def cacheKey: String = {
        transform match {
            case Some(value) => "%s:%s".format(source.cacheKey, value.cacheKey)
            case None => source.cacheKey
        }
    }
}


/** Cache uploaded pictures transformations between HTTP requests. */
@Singleton
class PictureCache @Inject() (
    val config: Configuration,
    pictureLoader: PictureLoader,
    )(implicit executionContext: TaskExecutionContext) {

    def get(source: PictureSource, transform: Option[PictureTransform] = None):
        Future[Option[SerializedPicture]] = {

        val key = PictureCacheKey(source, transform).cacheKey

        final case class PictureNotFoundException()
            extends Exception("Picture not found.")

        (cachingF(key)(ttl = None) {
            // If the image is not a transform, fetches it. Otherwise fetches the original first.
            (transform match {
                case None => pictureLoader.fromSource(source)
                case Some(value) => get(source, None).map(_.map(value.apply).map(_.serialized))
            }).flatMap {
                case Some(pic) => Future { pic }
                case None => Future.failed(PictureNotFoundException())
            }
        }).
            // Converts `pic` exceptions to `None`.
            map(Some(_)).recover { case _: PictureNotFoundException => None }
    }

    private implicit val cache: Cache[SerializedPicture] = {
        val maxCacheSize = config.underlying.
            getBytes("noisycamp.picturesMaxCacheSize")

        val underlying = Caffeine.newBuilder().
            maximumWeight(maxCacheSize).
            weigher(new Weigher[String, Entry[SerializedPicture]] {
                def weigh(key: String, picture: Entry[SerializedPicture]) = {
                    picture.value.bytes.length
                }
            }).
            build[String, Entry[SerializedPicture]]

        CaffeineCache(underlying)
    }
}
