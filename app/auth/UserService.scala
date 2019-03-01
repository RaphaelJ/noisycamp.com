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

import daos.{ UserDAO, UserLoginInfoDAO }
import models.{ User, UserLoginInfo }

class UserService @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider,
    val userDAO: UserDAO,
    val userLoginInfoDAO: UserLoginInfoDAO)
  (implicit executionContext: ExecutionContext)
  extends IdentityService[User]
  with HasDatabaseConfigProvider[JdbcProfile]
  {

  import profile.api._

  protected def userQuery(loginInfo: LoginInfo) = {
    for {
      userLoginInfo <- userLoginInfoDAO.get(loginInfo)
      user <- userDAO.query.filter(_.id === userLoginInfo.userID)
    } yield user
  }

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    db.run { userQuery(loginInfo).result.headOption }
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
        user <- userQuery(profile.loginInfo).
          result.
          headOption.
          flatMap {
            case Some(user) => DBIO.successful(user)
            case None => {
              // User does not exists.
              for {
                user <- userDAO.insert += User(
                  firstName = profile.firstName,
                  lastName = profile.lastName,
                  email = profile.email.get)
                _ <- userLoginInfoDAO.insert += UserLoginInfo(
                  userID = user.id,
                  loginProviderID = profile.loginInfo.providerID,
                  loginProviderKey = profile.loginInfo.providerKey)
              } yield user
            }
          }
      } yield user
    }
  }
}
