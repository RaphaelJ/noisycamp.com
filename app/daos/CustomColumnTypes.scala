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

import java.sql.Timestamp

import com.sksamuel.scrimage.Format
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

trait CustomColumnTypes {
  this: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  /** Stores a Joda's Datetime as an UTC Timestamp, in milliseconds. */
  implicit val jodaDateTimeType =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts.getTime, UTC)
    )
    
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