/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2021  Raphael Javaux <raphael@noisycamp.com>
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
import java.time.Instant
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import models.{ Plan, User, UserSubscription, UserSubscriptionStatus }

class UserSubscriptionDAO @Inject()
    (protected val dbConfigProvider: DatabaseConfigProvider)
    (implicit executionContext: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

    import profile.api._

    final class UserSubscriptionTable(tag: Tag)
        extends Table[UserSubscription](tag, "user_subscription") {

        def id                      = column[UserSubscription#Id]("id", O.PrimaryKey, O.AutoInc)
        def createdAt               = column[Instant]("created_at")

        def userId                  = column[User#Id]("user_id")

        def plan                    = column[Plan.Val]("plan")

        def stripeCheckoutSessionId = column[String]("stripe_checkout_session_id")
        def stripeCustomerId        = column[Option[String]]("stripe_customer_id")
        def stripeSubscriptionId    = column[Option[String]]("stripe_subscription_id")

        def status                  = column[Option[UserSubscriptionStatus.Value]]("status")

        def * = (
            id, createdAt, userId, plan,
            stripeCheckoutSessionId, stripeCustomerId, stripeSubscriptionId,
            status).mapTo[UserSubscription]
    }

    lazy val query = TableQuery[UserSubscriptionTable]

    lazy val insert = query returning
        query.map(_.id) into ((subscription, id) => subscription.copy(id=id))
}
