/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphaeljavaux@gmail.com>
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

import java.time.LocalDate
import javax.inject._

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api._
import play.api.mvc._
import play.api.http.SessionConfiguration
import play.api.libs.crypto.CSRFTokenSigner
import play.filters.csrf.{ CSRF, CSRFActionHelper, CSRFConfig }
import slick.ast.TypedType
import squants.market.Money
import squants.market

import auth.DefaultEnv
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import daos.CustomColumnTypes

@Singleton
class PayoutsController @Inject() (
    ccc: CustomControllerCompoments,

    csrfConfig: CSRFConfig,
    csrfTokenSigner: CSRFTokenSigner,
    sessionConfig: SessionConfiguration)
    extends CustomBaseController(ccc)
    with CustomColumnTypes {

    import profile.api._

    def index = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run((for {
            // Sums the total received bookings per currency, minus the payouts already received in
            // these currencies.
            balances <- {
                def orZero(nullable: Rep[Option[BigDecimal]]) = {
                    val coalesce = SimpleFunction.binary[
                        Option[BigDecimal], BigDecimal, BigDecimal]("coalesce")

                    coalesce(nullable, BigDecimal(0))
                }

                daos.studioBooking.query.
                    join(daos.studio.query).on(_.studioId === _.id).
                    filter { case (_, studio) => studio.ownerId === user.id }.
                    groupBy { case (booking, _) => booking.currency }.
                    map { case (currency, group) =>
                        val bookingTotal = group.
                            map { case (booking, _) => booking.total }.
                            sum

                        (orZero(bookingTotal), currency)
                    }.
                    map { case (total, currency) =>
                        val payoutTotal = daos.payout.query.
                            filter(_.customerId === user.id).
                            filter(_.currency === currency).
                            map { payout => payout.amount }.
                            sum

                        (total - orZero(payoutTotal), currency)
                    }.
                    result
            }

            payouts <- daos.payout.query.
                filter(_.customerId === user.id).
                result
        } yield (balances, payouts)).transactionally).
            map { case (balances, payouts) =>

                Ok(views.html.account.payouts(
                    identity=request.identity,
                    balances=balances.map { case (total, currency) => currency(total) },
                    nextPayout=LocalDate.now(),
                    payouts=payouts))
            }
    }

    /** Redirects the user to the Stripe OAuth onboarding flow. */
    def stripeOAuth = SecuredAction.async { implicit request =>
        val user = request.identity.user

        user.stripeAccountId.
            map { Future.successful _ }.
            getOrElse {
                // Creates a Stripe Express account if it does not exists.
                paymentService.createAccount(user).
                    flatMap { account =>
                        val stripeAccountId = account.getId
                        db.run {
                            daos.user.query.
                                filter(_.id === user.id).
                                map(_.stripeAccountId).
                                update(Some(stripeAccountId))
                        }.map { _ => stripeAccountId }
                    }
            }.
            flatMap { stripeAccountId =>
                val refreshUrl = routes.PayoutsController.stripeOAuth.absoluteURL
                var returnUrl = routes.PayoutsController.stripeOAuthComplete.absoluteURL

                paymentService.connectOAuthUrl(stripeAccountId, refreshUrl, returnUrl)
            }.
            map { url => Redirect(url) }
    }

    /** Processes the Stripe Connect OAuth response. */
    def stripeOAuthComplete = SecuredAction.async {
        implicit request =>

        val user = request.identity.user

        val redirectTo = routes.PayoutsController.index

        // Sets `user.stripeCompleted` to true if the `details_submitted` value of the associated
        // Stripe account is true.

        (user.stripeAccountId match {
            case Some(stripeAccountId) => {
                (paymentService.
                    retrieveAccount(stripeAccountId).
                    map { account => account.getDetailsSubmitted: Boolean })
            }
            case None => Future.successful(false)
        }).
            flatMap { isCompleted =>
                if (isCompleted) {
                    db.run {
                        daos.user.query.
                            filter(_.id === user.id).
                            map(_.stripeCompleted).
                            update(true)
                    }.
                        map { _ => 
                            Redirect(redirectTo).
                                flashing("success" -> (
                                    "Your account has been successfully setup. " + 
                                    "You can now receive online payments."))
                        }
                } else {
                    Future.successful(Redirect(redirectTo).
                        flashing("error" -> (
                            "Your account has not been correctly setup for online payments. " + 
                            "Please try again.")))
                }
            }
    }

    /** Redirects the user to the Stripe Express dashboard. */
    def stripeDashboard = SecuredAction.async { implicit request =>
        val user = request.identity.user

        user.stripeAccountId match {
            case Some(stripeAccountId) if user.stripeCompleted => {
                paymentService.
                connectDashboardUrl(stripeAccountId).
                map { TemporaryRedirect _ }
            }
            case _ => Future { BadRequest("No Stripe account connected.") }
        }
    }
}
