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
import forms.components.CountryForm
import _root_.i18n.Country

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

        user.stripeAccountId match {
            case Some(stripeAccountId) if user.isPayoutSetup => {
                stripeDashboardRedirect(stripeAccountId)
            }
            case Some(stripeAccountId) => stripeOAuthRedirect(stripeAccountId)
            case None => Future.successful(Redirect(routes.PayoutsController.setup))
        }
    }

    def setup = SecuredAction.async { implicit request =>
        val user = request.identity.user

        user.stripeAccountId match {
            case Some(accountId) => Future.successful(Redirect(routes.PayoutsController.index))
            case None => {
                CountryForm.form.bindFromRequest().fold(
                    form => Future.successful(
                        Ok(views.html.account.payouts.setup(
                            identity=request.identity, countryForm=form))),
                    country => {
                        // Creates a Stripe Express account and redirects the use to the Stripe
                        // onboarding flow for the selected country.
                        paymentService.createAccount(user, country).
                            flatMap { account =>
                                val stripeAccountId = account.getId
                                db.run {
                                    daos.user.query.
                                        filter(_.id === user.id).
                                        map(_.stripeAccountId).
                                        update(Some(stripeAccountId))
                                }.map { _ => stripeAccountId }
                            }.
                            flatMap { stripeOAuthRedirect _ }
                    }
                )
            }
        }
    }

    /** Processes the Stripe Connect OAuth response. */
    def setupComplete = SecuredAction.async { implicit request =>
        val user = request.identity.user
        val redirectTo = Redirect(routes.IndexController.index)

        // Sets `user.stripeCompleted` to true if the `details_submitted` value of the associated
        // Stripe account is true.

        (user.stripeAccountId match {
            case Some(stripeAccountId) => {
                paymentService.
                    retrieveAccount(stripeAccountId).
                    map { account => account.getDetailsSubmitted: Boolean }
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
                            redirectTo.
                                flashing("success" -> (
                                    "Your account has been successfully setup. " +
                                    "You can now receive online payments."))
                        }
                } else {
                    Future.successful(redirectTo.
                        flashing("error" -> (
                            "Your account has not been correctly setup for online payments. " +
                            "Please try again.")))
                }
            }
    }

    private def stripeOAuthRedirect(stripeAccountId: String)(implicit req: RequestHeader):
        Future[Result] = {

        val refreshUrl = routes.PayoutsController.index.absoluteURL(true)
        var returnUrl = routes.PayoutsController.setupComplete.absoluteURL(true)

        paymentService.
            connectOAuthUrl(stripeAccountId, refreshUrl, returnUrl).
            map { TemporaryRedirect _ }
    }

    private def stripeDashboardRedirect(stripeAccountId: String)(implicit req: RequestHeader):
        Future[Result] = {

        paymentService.
            connectDashboardUrl(stripeAccountId).
            map { TemporaryRedirect _ }
    }
}
