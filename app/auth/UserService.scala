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

import scala.concurrent.{ ExecutionContext, Future }

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.{ IdentityService }
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import javax.inject._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import daos.UserDAO
import models.User

class UserService @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
  )
  (userDAO: UserDAO)(implicit executionContext: ExecutionContext)
  extends IdentityService[User]
  with HasDatabaseConfigProvider[JdbcProfile]
  {

  import profile.api._

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    db.run {
      userDAO.get(loginInfo)
    }
  }

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a
   * new user with the given profile.
   */
  def save(profile: CommonSocialProfile): Future[User] = {
    db.run {
      for {
        userOption <- userDAO.get(profile.loginInfo)
        user <- userOption match {
          case Some(user) => DBIO.successful(user)
          case None => {
            // User does not exist, creates it.
            (userDAO.users
              returning userDAO.users.map(_.id)
              into ((user, userId) => user.copy(id=userId))
            ) += User(
              email = profile.email.get,
              loginProviderId = profile.loginInfo.providerID,
              loginProviderKey = profile.loginInfo.providerKey)
          }
        }
      } yield user
    }
  }
}
