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

package i18n

import javax.inject._
import scala.concurrent.{ ExecutionContext, Future, blocking }

import com.github.benmanes.caffeine.cache.{ Caffeine, Weigher }
import play.api.Configuration
import play.api.libs.ws._
import scalacache._
import scalacache.caffeine._
import scalacache.modes.scalaFuture._

import misc.TaskExecutionContext

/** The result of an IP location lookup. */
case class GeoIpLocation(country: Country.Val)

/** Provides a service that retreives location information from an IP address.
 *
 * Previous results are cached for efficiency.
 */
@Singleton
class GeoIpService @Inject() (
  val config: Configuration,
  val ws: WSClient,
  implicit val executionContext: TaskExecutionContext) {

  def get(ip: String): Future[Option[GeoIpLocation]] = {
    cachingF(ip)(ttl = None) {
      val url = f"https://get.geojs.io/v1/ip/geo/${ip}.json"

      ws.url(url).
        withFollowRedirects(true).
        get.
        map { response =>
          (response.json \ "country_code").
            asOpt[String].
            flatMap { code => Country.byCode.get(code) }.
            map(GeoIpLocation(_))
        }
    }
  }

  private implicit val cache: Cache[Option[GeoIpLocation]] = {
    val expireAfter = config.underlying.
      getDuration("noisycamp.geoIpCacheExpireAfter")
    val maxCacheSize = config.underlying.
      getBytes("noisycamp.geoIpMaxCacheSize")

    val underlying = Caffeine.newBuilder().
      expireAfterWrite(expireAfter).
      maximumSize(maxCacheSize).
      build[String, Entry[Option[GeoIpLocation]]]

    CaffeineCache(underlying)
  }
}
