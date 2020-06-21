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

  def index = silhouette.SecuredAction.async { implicit request =>

    val user = request.identity.user

    var redirectUrl = routes.PayoutsController.stripeOAuthRedirect("", "").
      absoluteURL.
      // Removes the query string from the URL, as these will be added by
      // Stripe.
      takeWhile(_ != '?')

    val stripeOAuthUrl = paymentService.connectOAuthUrl(user, redirectUrl)

    db.run((for {
      // Sums the total received bookings per currency, minus the payouts
      // already received in these currencies.
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
          stripeOAuthUrl=stripeOAuthUrl,
          payouts=payouts))
      }
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
