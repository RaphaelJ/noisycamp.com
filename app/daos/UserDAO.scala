/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2020  Raphael Javaux <raphael@noisycamp.com>
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
import java.time.Instant
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import models.{ Plan, User, UserSubscription }

class UserDAO @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider)
    (implicit executionContext: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

    import profile.api._

    final class UserTable(tag: Tag) extends Table[User](tag, "user") {

        def id                  = column[Long]("id", O.PrimaryKey, O.AutoInc)
        def createdAt           = column[Instant]("created_at")
        def firstName           = column[Option[String]]("first_name")
        def lastName            = column[Option[String]]("last_name")
        def email               = column[String]("email")
        def avatarId            = column[Option[Long]]("avatar_id")

        def plan                = column[Plan.Val]("plan")
        def subscriptionId      = column[Option[UserSubscription#Id]]("subscription_id")

        def stripeAccountId     = column[Option[String]]("stripe_account_id")
        def stripeCompleted     = column[Boolean]("stripe_completed")

        def * = (id, createdAt, firstName, lastName, email, avatarId, plan, subscriptionId,
            stripeAccountId, stripeCompleted).mapTo[User]
    }

    lazy val query = TableQuery[UserTable]

    lazy val insert = query returning
        query.map(_.id) into ((user, id) => user.copy(id=id))
}
