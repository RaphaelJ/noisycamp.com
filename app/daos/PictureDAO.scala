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

import scala.concurrent.{ ExecutionContext, Future }
import java.nio.file.Files
import javax.inject.Inject

import com.sksamuel.scrimage.Format
import org.joda.time.DateTime
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import models.Picture

class PictureDAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class PictureTable(tag: Tag) extends Table[Picture](tag, "picture") {

    // ID is the SHA-256 hashed content of the file.
    def id                = column[Array[Byte]]("id", O.PrimaryKey)
    def uploadedAt        = column[DateTime]("uploaded_at")
    def format            = column[Format]("format")
    def content           = column[Array[Byte]]("content")

    def * = (id, uploadedAt, format, content).mapTo[Picture]
  }

  lazy val query = TableQuery[PictureTable]
  
  def get(id: Array[Byte]) = query.filter(_.id === id)

  /** Inserts the picture in the database with its hash as ID if it does not
   * exists and returns it. Returns the existing picture otherwise. */
  def insertIfNotExits(picture: Picture): Future[Picture] = {
    db.run({
      get(picture.id).
        result.
        headOption.
        flatMap {
          case Some(oldPicture) => DBIO.successful(oldPicture)
          case None => for { _ <- query += picture } yield picture
        }
      }.transactionally)
  }
}
