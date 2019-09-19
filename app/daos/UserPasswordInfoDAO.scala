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
import scala.reflect.ClassTag
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import models.{ UserPasswordInfo, UserLoginInfo }

class UserPasswordInfoDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  val userLoginInfosDAO: UserLoginInfoDAO
  )(
    implicit executionContext: ExecutionContext,
    implicit val classTag: ClassTag[PasswordInfo]
  )
  extends DelegableAuthInfoDAO[PasswordInfo]
  with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  final class UserPasswordInfoTable(tag: Tag)
    extends Table[UserPasswordInfo](tag, "user_password_info") {

    def loginInfoID       = column[Long]("login_info_id", O.PrimaryKey)
    def hasher            = column[String]("hasher")
    def password          = column[String]("password")
    def salt              = column[Option[String]]("salt")

    def * = (loginInfoID, hasher, password, salt).mapTo[UserPasswordInfo]

    def userLoginInfo = foreignKey(
      "fk_user_password_info_login_info_id", loginInfoID,
      userLoginInfosDAO.query)(_.id)
  }

  lazy val query = TableQuery[UserPasswordInfoTable]

  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    db.run {
      userLoginInfosDAO.
        get(loginInfo).
        join(query).
          on (_.id === _.loginInfoID).
        map(_._2).
        result.
        headOption
    }.map(_.map(_.passwordInfo))
  }

  protected def addAction(loginInfo: LoginInfo, passInfo: PasswordInfo) = {
    for {
      userLoginInfoId <- userLoginInfosDAO.get(loginInfo).map(_.id).result.head
      ret <- query += UserPasswordInfo(
        userLoginInfoId, passInfo.hasher, passInfo.password, passInfo.salt)
    } yield passInfo
  }

  protected def updateAction(loginInfo: LoginInfo, passInfo: PasswordInfo) = {
    for {
      userLoginInfoId <- userLoginInfosDAO.get(loginInfo).map(_.id).result.head
      _ <- query.
        filter(_.loginInfoID === userLoginInfoId).
        map(v => (v.hasher, v.password, v.salt)).
        update((passInfo.hasher, passInfo.password, passInfo.salt))
    } yield passInfo
  }

  def add(loginInfo: LoginInfo, passInfo: PasswordInfo)
    : Future[PasswordInfo] = {

    db.run(addAction(loginInfo, passInfo).transactionally)
  }

  def update(loginInfo: LoginInfo, passInfo: PasswordInfo)
    : Future[PasswordInfo] = {

    db.run(updateAction(loginInfo, passInfo).transactionally)
  }

  /**
   * Saves the auth info for the given login info.
   *
   * This method either adds the auth info if it doesn't exists or it updates
   * the auth info if it already exists.
   */
  def save(loginInfo: LoginInfo, passInfo: PasswordInfo)
    : Future[PasswordInfo] = {

    val action =
      for {
        userLoginInfoId <- userLoginInfosDAO.
          get(loginInfo).
          map(_.id).
          result.head

        _ <- query.
          filter(_.loginInfoID === userLoginInfoId).
          result.
          headOption.
          flatMap {
            case Some(_) => updateAction(loginInfo, passInfo)
            case None => addAction(loginInfo, passInfo)
          }
      } yield passInfo

    db.run(action.transactionally)
  }

  def remove(loginInfo: LoginInfo): Future[Unit] = {
    val action =
      for {
        userLoginInfoId <- userLoginInfosDAO.
          get(loginInfo).
          map(_.id).
          result.head

        _ <- query.
          filter(_.loginInfoID === userLoginInfoId).
          delete
      } yield ()

    db.run(action.transactionally)
  }
}
