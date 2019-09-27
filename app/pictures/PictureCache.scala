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

import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

import com.github.benmanes.caffeine.cache.{ Caffeine, Weigher }
import play.api.Configuration
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import scalacache._
import scalacache.caffeine._
import scalacache.modes.scalaFuture._
import slick.jdbc.JdbcProfile

import models.{ Picture, PictureId }
import daos.PictureDAO

case class PictureCacheKey(id: PictureId, transform: PictureTransform)

/** Cache uploaded pictures transformations between HTTP requests. */
@Singleton
class PictureCache @Inject() (
  val config: Configuration,
  protected val dbConfigProvider: DatabaseConfigProvider,
  pictureDao: PictureDAO
  )
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def get(id: PictureId, transform: PictureTransform)
    : Future[Option[Picture]] = {

    val key = PictureCacheKey(id, transform).toString

    final case class PictureNotFoundException()
      extends Exception("Picture not found.")

    val pic: Future[Picture] = cachingF(key)(ttl = None) {
      // Picture not in cache, fetches it from the database.
      // TODO: don't query the DB if the raw picture is in cache.
      val optPic: Future[Option[Picture]] = db.run {
        pictureDao.get(id).
          result.
          headOption
      }

      optPic.flatMap {
        case Some(pic) => Future { transform(pic) }
        case None => Future.failed(PictureNotFoundException())
      }
    }

    // Converts `pic` exceptions to `None`.
    pic.map(Some(_)).recover { case _: PictureNotFoundException => None }
  }

  private implicit val cache: Cache[Picture] = {
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
