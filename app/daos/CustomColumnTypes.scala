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

package daos

import java.time.{ Duration, LocalTime }

import com.sksamuel.scrimage.Format
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import misc.Country

trait CustomColumnTypes {
  this: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  /** Maps a country value to its ISO code. */
  implicit val countryVal =
    MappedColumnType.base[Country.Val, String](_.isoCode, Country.byCode(_))

  /** Stores a java.time.Duration as an amount of milliseconds. */
  implicit val duration =
    MappedColumnType.base[Duration, Long](_.toMillis, Duration.ofMillis)

  /** Alternative implementation of java.time.LocalTime that maps to a string
   *
   * See <https://github.com/tminglei/slick-pg/issues/381> */
  implicit val localtime =
    MappedColumnType.base[LocalTime, String](_.toString, LocalTime.parse)

  /** Stores Scrimage image format as text */
  implicit val scrimageFormat =
    MappedColumnType.base[Format, String](
      {
        case Format.PNG => "png"
        case Format.GIF => "gif"
        case Format.JPEG => "jpeg"
      },
      {
        case "png" => Format.PNG
        case "gif" => Format.GIF
        case "jpeg" => Format.JPEG
      }
    )
}
