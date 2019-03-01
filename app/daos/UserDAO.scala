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

import scala.concurrent.ExecutionContext
import javax.inject.Inject

import org.joda.time.DateTime
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import models.User

class UserDAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  final class UserTable(tag: Tag) extends Table[User](tag, "user") {

    def id                = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def firstName         = column[Option[String]]("first_name")
    def lastName          = column[Option[String]]("last_name")
    def email             = column[String]("email")

    def * = (id, firstName, lastName, email).mapTo[User]
  }

  lazy val query = TableQuery[UserTable]

  lazy val insert = query returning
    query.map(_.id) into ((user, id) => user.copy(id=id))
}
