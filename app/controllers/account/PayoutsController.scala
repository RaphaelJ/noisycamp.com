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
import squants.market.Money

import auth.DefaultEnv
import _root_.i18n.Currency
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }

@Singleton
class PayoutsController @Inject() (
  ccc: CustomControllerCompoments,

  csrfConfig: CSRFConfig,
  csrfTokenSigner: CSRFTokenSigner,
  sessionConfig: SessionConfiguration)
  extends CustomBaseController(ccc) {

  import profile.api._

  def index = silhouette.SecuredAction { implicit request =>
    var redirectUrl = routes.PayoutsController.stripeOAuthRedirect("", "").
      absoluteURL.
      // Removes the query string from the URL
      takeWhile(_ != '?')

    val stripeOAuthUrl =
      paymentService.connectOAuthUrl(request.identity.user, redirectUrl)

    Ok(views.html.account.payouts(
      identity=request.identity,
      currentBalance=Currency.EUR(1230.12),
      nextPayout=LocalDate.now(),
      stripeOAuthUrl=stripeOAuthUrl))
  }

  /** Processes the Stripe Connect OAuth response.  */
  def stripeOAuthRedirect(state: String, code: String) =
    silhouette.SecuredAction.async { implicit request =>

    // The `state` parameter is the CSRF token. Validates the state based on
    // Play's internal token.
    val tokenProvider =
      new CSRF.TokenProviderProvider(csrfConfig, csrfTokenSigner).get
    val refToken =
      new CSRFActionHelper(sessionConfig, csrfConfig, csrfTokenSigner).
        getTokenToValidate(request).
        get
    val isTokenValid = tokenProvider.compareTokens(refToken, state)

    val user = request.identity.user

    if (!isTokenValid) {
      Future.successful(BadRequest("Invalid `state` parameter."))
    } else {
      // Sets the Stripe User ID from the returned value from the Stripe API.
      paymentService.
        // Retreives the account ID from the API.
        connectOAuthComplete(code).
        map { _.getStripeUserId }.
        // Saves the account ID in the database.
        flatMap { stripeUserId =>
          db.run {
            daos.user.query.
              filter(_.id === user.id).
              map(_.stripeUserId).
              update(Some(stripeUserId))
          }
        }.
        map { _ => Redirect(routes.PayoutsController.index) }
    }
  }

  /** Redirects the user to the Stripe Express dashboard. */
  def stripeDashboard = silhouette.SecuredAction.async { implicit request =>

    val user = request.identity.user

    user.stripeUserId match {
      case None => Future { BadRequest("No Stripe account connected.") }
      case Some(stripeUserId) => {
        paymentService.
          connectDashboardUrl(stripeUserId).
          map { TemporaryRedirect _ }
      }
    }
  }
}
