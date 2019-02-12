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

package auth

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.{ IdentityService }
import javax.inject._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import daos.UserDAO
import models.User

class UserService @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
  )
  (userDAO: UserDAO)
  extends IdentityService[User]
  with HasDatabaseConfigProvider[JdbcProfile]
  {

  import profile.api._

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    db.run {
      userDAO.users.
        filter(_.loginProviderId === loginInfo.providerID).
        filter(_.loginProviderKey === loginInfo.providerKey).
        result.
        headOption
    }
  }
}
