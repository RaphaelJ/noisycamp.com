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

package models

import java.time.Instant

object UserSubscriptionStatus extends Enumeration {
    // The subscription is currently in the trial period.
    val Trialing = Value

    // The subscription is currently active and the latest payment was successful.
    val Active = Value

    // The subscription payment failed during the creation of the subscription.
    val Incomplete = Value

    // The subscription payment failed during the creation of the subscription, and cannot be done
    // anymore.
    val IncompleteExpired = Value

    // The latest subscription payment failed, but Stripe still attemps to recover payment.
    val PastDue = Value

    val Cancelled = Value

    // The latest invoice hasn't been paid.
    val Unpaid = Value

    /** Deduces the subscription status from the Stripe's subscription status value. */
    def fromStripeValue(value: String) = {
        value match {
            case "trialing" => Trialing
            case "active" => Active
            case "incomplete" => Incomplete
            case "incomplete_expired" => IncompleteExpired
            case "past_due" => PastDue
            case "canceled" => Cancelled
            case "unpaid" => Unpaid
            case _ => throw new IllegalArgumentException(f"Invalid Stripe status ('${value}')")
        }
    }
}

case class UserSubscription(
    id:                         UserSubscription#Id = 0L,
    createdAt:                  Instant = Instant.now(),

    userId:                     User#Id,

    plan:                       Plan.Val,

    stripeCheckoutSessionId:    String,
    stripeCustomerId:           Option[String] = None,
    stripeSubscriptionId:       Option[String] = None,

    status:                     Option[UserSubscriptionStatus.Value] = None) {

    type Id = Long

    /** The subscription is currently valid and the resources for it can be allocated. */
    def isActive = {
        Seq(
            UserSubscriptionStatus.Trialing,
            UserSubscriptionStatus.Active,
            UserSubscriptionStatus.PastDue,
            ).
            map(Some(_)).
            contains(status)
    }

    /** The subscription is either cancelled or expired due to incomplete payment. */
    def isEnded = {
        Seq(
            UserSubscriptionStatus.IncompleteExpired,
            UserSubscriptionStatus.Cancelled,
            UserSubscriptionStatus.Unpaid,
            ).
            map(Some(_)).
            contains(status)
    }
}

