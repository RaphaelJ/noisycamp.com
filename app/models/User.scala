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

package models

import java.time.Instant

import play.api.Configuration

import misc.UniqueId

/** Stores the information about an user. */
case class User(
    id:                 User#Id = 0L,
    createdAt:          Instant = Instant.now(),
    firstName:          Option[String],
    lastName:           Option[String],
    email:              String,
    avatarId:           Option[Long],

    plan:               Plan.Val = Plan.Free,
    subscriptionId:     Option[UserSubscription#Id] = None,
    nextSubscriptionId: Option[UserSubscription#Id] = None,

    stripeAccountId:    Option[String] = None,
    stripeCompleted:    Boolean = false) {

    require(!stripeCompleted || stripeAccountId.isDefined)

    type Id = Long

    def fullName: Option[String] = (firstName, lastName) match {
        case (Some(fn), Some(ln)) => Some(fn + ' ' + ln)
        case _ => None
    }

    /** Returns the full-name if availaible, or else the email. */
    def displayName: String = fullName.getOrElse(email)

    /** True if can do accept online payments. */
    def isPayoutSetup: Boolean = stripeAccountId.isDefined && stripeCompleted

    /** True if the user plan is currently being upgraded. */
    def isUpgrading: Boolean = nextSubscriptionId.isDefined

    /** Generates a random but deterministic secret string that can be used to securely access
     * user's resources. */
    def secret(implicit config: Configuration): String = {
        val SALT = "user-secret"
        UniqueId.generate(SALT, id.toString)
    }
}
