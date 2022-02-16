/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphael@noisycamp.com>
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

package controllers.account

import javax.inject._

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.stripe.Stripe
import com.stripe.model.{ checkout, Subscription }
import play.api.mvc._
import squants.market.Currency

import auth.DefaultEnv
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import daos.CustomColumnTypes
import forms.account.PremiumForm
import models.{
    FacebookEvent, FacebookEventName, FacebookContentCategory, FacebookContentIds, FacebookValue,
    FacebookCurrency, FacebookContentType, FacebookContentTypeValues, FacebookPredictedLtv,
    Plan, UserSubscription, User, UserSubscriptionStatus }

@Singleton
class PlansController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc) with CustomColumnTypes {

    import profile.api._

    //
    // Endpoints
    //

    def index = UserAwareAction.async { implicit request =>
        val userOpt = request.identity.map(_.user)

        for {
            currency <- clientCurrency
            hasFreeTrial <- db.run(hasFreeTrial(userOpt))
        } yield {
            val fbEvent = request.flash.get("redirect-from") match {
                case Some("payment-success") => {
                    FacebookEvent(
                        FacebookEventName.Subscribe,
                        Seq(
                            FacebookCurrency(currency),
                            FacebookPredictedLtv(
                                userOpt.map(_.plan.ltv(currency).amount).getOrElse(BigDecimal(0)))))
                }
                case _ => {
                    FacebookEvent(
                        FacebookEventName.Lead,
                        Seq(FacebookContentCategory("plan")))
                }
            }

            Ok(views.html.account.plans.index(
                request.identity, currency, hasFreeTrial, facebookEvent = Some(fbEvent)))
        }
    }

    /** Initiates an upgrade transaction for the given Plan. */
    def upgrade(planCode: String) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        Plan.byCode.get(planCode) match {
            case Some(plan) => {
                db.run({
                    for {
                        user <- cancelNextSubscription(user)
                        currency <- DBIO.from(clientCurrency)
                        hasFreeTrial <- hasFreeTrial(Some(user))

                        result <-
                            if (plan.isFree) {
                                for {
                                    _ <- setDefaultUserPlan(user)
                                } yield Redirect(routes.PlansController.index).
                                    flashing(
                                        "success" -> "Your plan has been successfully upgraded.")
                            } else {
                                val fbEvent = FacebookEvent(
                                    FacebookEventName.InitiateCheckout,
                                    Seq(
                                        FacebookContentCategory("plan"),
                                        FacebookContentType(FacebookContentTypeValues.Product),
                                        FacebookContentIds(Seq(planCode)),
                                        FacebookValue(plan.ltv(currency).amount),
                                        FacebookCurrency(currency)))

                                createSubscription(user, plan, currency).
                                    map { case (_, session) =>
                                        Ok(views.html.stripeCheckoutRedirect(
                                            identity = Some(request.identity),
                                            stripeSession = session,
                                            facebookEvent = Some(fbEvent)))
                                    }
                            }
                    } yield result
                }.transactionally)
            }
            case None => {
                Future.successful(
                    Redirect(routes.PlansController.index).
                        flashing("error" -> "This plan does not exist"))
            }
        }
    }

    /** Abort an ongoing subsription upgrade process. */
    def upgradeAbort = SecuredAction.async { implicit request =>
        val user = request.identity.user

        val redirect = Redirect(routes.PlansController.index)

        db.run(cancelNextSubscription(user).transactionally).
            map { _ =>
                Redirect(routes.PlansController.index).
                    flashing("success" -> "You plan upgrade has been successfully cancelled.")
            }
    }

    def paymentSuccess(sessionId: String) = SecuredAction.async {
        implicit request =>

        val user = request.identity.user

        for {
            session <- paymentService.retrieveSession(sessionId)
            _ <- handleCheckoutSessionCompleted(session)
        } yield {
            Redirect(routes.PlansController.index).
                flashing(
                    "success" -> "You payment has been successfully processed.",
                    "redirect-from" -> "payment-success")
        }
    }

    //
    // Webhooks events
    //

    def handleCheckoutSessionCompleted(session: checkout.Session): Future[Result] = {
        for {
            stripeSubscription <- paymentService.
                retrieveSubscription(session.getSubscription)

            result <- db.run({
                for {
                    subscriptionOpt <- daos.userSubscription.query.
                        filter(_.stripeCheckoutSessionId === session.getId).
                        result.
                        headOption

                    result <- handleOptSubscriptionUpdate(
                        subscriptionOpt, stripeSubscription)
                } yield result
            }.transactionally)
        } yield result
    }

    def handleSubscriptionUpdated(stripeSubscription: Subscription): Future[Result] = {
        db.run({
            for {
                subscriptionOpt <- daos.userSubscription.query.
                    filter(_.stripeSubscriptionId === stripeSubscription.getId).
                    result.
                    headOption

                result <- handleOptSubscriptionUpdate(
                    subscriptionOpt, stripeSubscription)
            } yield result
        }.transactionally)
    }

    private def handleOptSubscriptionUpdate(
        subscriptionOpt: Option[UserSubscription], stripeSubscription: Subscription):
        DBIO[Result] = {

        subscriptionOpt match {
            case Some(subscription) => {
                updateFromSubscription(subscription, stripeSubscription).
                    map { _ => Ok("subscription-updated") }
            }
            case None => DBIO.successful(NotFound("subscription-not-found"))
        }
    }

    //
    // Utilities
    //

    /** Returns `true` if the user is eligible to a free trial. */
    def hasFreeTrial(userOpt: Option[User]): DBIO[Boolean] = {
        userOpt match {
            case Some(user) => {
                val status =
                    UserSubscriptionStatus.activeValues ++
                    UserSubscriptionStatus.completedValues

                daos.userSubscription.query.
                    filter(_.userId === user.id).
                    filter(_.status inSet status).
                    exists.
                    result.
                    map(!_)
            }
            case None => DBIO.successful(true)
        }
    }

    /** Sets the current user plan, using the optional provided subscription.
     *
     * Cancels any ongoing subscription, and remove the next subscription value if it matches the
     * provided subscription.
     */
    def setUserPlan(user: User, planOrSubscription: Either[Plan.Val, UserSubscription]):
        DBIO[User] = {

        require(planOrSubscription.isRight || planOrSubscription.left.get.isFree)

        val (plan, subscriptionId, nextSubscriptionId) = planOrSubscription match {
            case Left(plan) => (plan, None, user.nextSubscriptionId)
            case Right(subscription) => {
                val nextSubscriptionId =
                    if (Some(subscription.id) == user.nextSubscriptionId) None
                    else user.nextSubscriptionId
                (subscription.plan, Some(subscription.id), nextSubscriptionId)
            }
        }

        val newUser = user.copy(
            plan = plan,
            subscriptionId = subscriptionId,
            nextSubscriptionId = nextSubscriptionId)

        for {
            _ <- cancelCurrentSubscription(user)

            _ <- daos.user.query.
                filter(_.id === user.id).
                map(u => (u.plan, u.subscriptionId, u.nextSubscriptionId)).
                update((plan, subscriptionId, nextSubscriptionId))

            _ <- applyPlanLimits(newUser)
        } yield newUser
    }

    def setDefaultUserPlan(user: User): DBIO[User] = setUserPlan(user, Left(Plan.default))

    /** Applies the current user's plan limits. */
    def applyPlanLimits(user: User): DBIO[Unit] = {
        // Unpublish user's studios until no more than the limit are published.
        def applyStudioLimit = {
            user.plan.
                studioLimit.
                map { newLimit =>
                    // Ensures that no more than `newLimit` studios are currently published.
                    for {
                        published <- daos.studio.publishedStudios.
                            filter(_.ownerId === user.id).
                            sortBy(_.id).
                            map(_.id).
                            result

                        _ <- daos.studio.query.
                            filter(_.id inSet published.drop(newLimit)).
                            map(_.published).
                            update(false)
                    } yield Unit
                }.
                getOrElse(DBIO.successful(Unit))
        }

        // Disable onsite payments on the user's studios if disabled. */
        def applyOnsitePayment = {
            if (!user.plan.onsitePayments) {
                // Disables onsite payments for all studios.
                daos.studio.query.
                    filter(_.ownerId === user.id).
                    map(_.hasOnsitePayment).
                    update(false)
            } else DBIO.successful(Unit)
        }

        // Removes any pricing on the user's equipments if disabled. */
        def applyEquipmentFee = {
            if (!user.plan.equipmentFee) {
                for {
                    // User's equipments that require a fee.
                    equips <- daos.studio.query.
                        filter(_.ownerId === user.id).
                        join(daos.studioEquipment.query).
                            on(_.id === _.studioId).
                        join(daos.equipment.query).
                            on(_._2.equipmentId === _.id).
                        filter(_._2.priceType.isDefined).
                        map(v => (v._1._2.id, v._2)).
                        result

                    // Makes a copies of every priced equipment of the user.
                    uniqueEquips = equips.map(_._2).distinct
                    newEquips <- daos.equipment.insert ++=
                        uniqueEquips.map(_.copy(id=0L, price=None))
                    priceIdsMap = uniqueEquips.map(_.id).zip(newEquips.map(_.id)).toMap

                    // Points the user's studios to the new, not priced, equipment objects.
                    _ <- DBIO.sequence(equips.map { case (studioEquipId, oldEquip) =>
                        daos.studioEquipment.query.
                            filter(_.id === studioEquipId).
                            map(_.equipmentId).
                            update(priceIdsMap(oldEquip.id))
                    })
                } yield Unit
            } else DBIO.successful(Unit)
        }

        for {
            _ <- applyStudioLimit
            _ <- applyOnsitePayment
            _ <- applyEquipmentFee
        } yield Unit
    }

    def createSubscription(user: User, plan: Plan.Value, currency: Currency)(
        implicit request: RequestHeader):
        DBIO[(UserSubscription, checkout.Session)] = {

        require(!plan.isFree)

        val onSuccessEscaped = routes.PlansController.paymentSuccess("{CHECKOUT_SESSION_ID}")
        // De-escape { and } characters
        val onSuccess = onSuccessEscaped.copy(
            url=onSuccessEscaped.url.replaceAll("%7B", "{").replaceAll("%7D", "}"))

        val metadata = Map(
            "charge_type" -> "plan",
            "plan" -> plan.code,
        )

        val userQuery = daos.user.query.filter(_.id === user.id)

        for {
            hasFreeTrial <- hasFreeTrial(Some(user))
            trial = if (hasFreeTrial) Some(14) else None

            _ <- cancelNextSubscription(user)

            session <- DBIO.from(paymentService.createSubscriptionSession(
                user, plan, currency, trial,
                onSuccess, routes.PlansController.index, metadata))

            subscription <- daos.userSubscription.
                insert += UserSubscription(
                    userId                  = user.id,
                    plan                    = plan,
                    stripeCheckoutSessionId = session.getId)

            _ <- daos.user.query.
                filter(_.id === user.id).
                map(_.nextSubscriptionId).
                update(Some(subscription.id))
        } yield (subscription, session)
    }

    /** Updates the user subscription object based on the new Stripe subscription's state. */
    def updateFromSubscription(subscription: UserSubscription, stripeSubscription: Subscription):
        DBIO[UserSubscription] = {

        val newStatus = UserSubscriptionStatus.fromStripeValue(stripeSubscription.getStatus)
        val stripeCustomerId = stripeSubscription.getCustomer
        val stripeSubscriptionId = stripeSubscription.getId

        val userQuery = daos.user.query.filter(_.id === subscription.userId)
        val subscriptionQuery = daos.userSubscription.query.filter(_.id === subscription.id)

        for {
            // Updates the UserSubscription object
            _ <- subscriptionQuery.
                map(s => (s.status, s.stripeCustomerId, s.stripeSubscriptionId)).
                update((Some(newStatus), Some(stripeCustomerId), Some(stripeSubscriptionId)))
            newSubscription <- subscriptionQuery.result.head

            // Possibly updates the User's plan when required.
            user <- userQuery.result.head
            _ <- {
                val isCurrentSubscription = Some(subscription.id) == user.subscriptionId
                val isNextSubscription = Some(subscription.id) == user.nextSubscriptionId

                if (isCurrentSubscription && newSubscription.isEnded) {
                    // This is the current subscription and it failed, resets to default plan.
                    setDefaultUserPlan(user)
                } else if (isNextSubscription) {
                    if (newSubscription.isActive) {
                        // Replaces the current subscription
                        setUserPlan(user, Right(newSubscription))
                    } else if (newSubscription.isEnded) {
                        // New subscription failed.
                        cancelNextSubscription(user)
                    } else DBIO.successful(Unit)
               } else DBIO.successful(Unit)
            }
        } yield newSubscription
    }

    /** Cancels and detach any current subscription for the provided user.
     *
     * Does not upgrade/downgrade the current user's plan.
     *
     * @return returns the upgraded user object.
     */
    def cancelCurrentSubscription(user: User): DBIO[User] = {
        user.subscriptionId match {
            case Some(subscriptionId) => {
                for {
                    subscription <- daos.userSubscription.query.
                        filter(_.id === subscriptionId).
                        result.head
                    _ <- cancelSubscription(subscription)

                    _ <- daos.user.query.
                        filter(_.id === user.id).
                        map(_.subscriptionId).
                        update(None)
                } yield user.copy(subscriptionId = None)
            }
            case None => DBIO.successful(user)
        }
    }

    /** Cancels any upgrade transaction in progress for the provided user.
     *
     * @return returns the upgraded user object.
     */
    def cancelNextSubscription(user: User): DBIO[User] = {
        user.nextSubscriptionId match {
            case Some(nextSubscriptionId) => {
                for {
                    subscription <- daos.userSubscription.query.
                        filter(_.id === nextSubscriptionId).
                        result.head
                    _ <- cancelSubscription(subscription)

                    _ <- daos.user.query.
                        filter(_.id === user.id).
                        map(_.nextSubscriptionId).
                        update(None)
                } yield user.copy(nextSubscriptionId = None)
            }
            case None => DBIO.successful(user)
        }
    }

    /** Cancels the provided subscription if it's still active.
     *
     * @return the newly updated UserSubscription object.
     */
    def cancelSubscription(subscription: UserSubscription): DBIO[UserSubscription] = {
        subscription.stripeSubscriptionId match {
            case Some(stripeSubscriptionId) if subscription.isActive => {
                val query = daos.userSubscription.query.
                    filter(_.stripeSubscriptionId === stripeSubscriptionId)

                for {
                    stripeSubscription <- DBIO.from(
                        paymentService.retrieveSubscription(stripeSubscriptionId))
                    _ <- DBIO.from(paymentService.cancelSubscription(stripeSubscription))

                    _ <- query.
                        map(s => s.status).
                        update(Some(UserSubscriptionStatus.Cancelled))

                    subscription <- query.result.head
                } yield subscription
            }
            case _ => DBIO.successful(subscription)
        }
    }
}
