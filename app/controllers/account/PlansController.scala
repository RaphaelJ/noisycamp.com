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

import com.stripe.Stripe
import play.api.mvc._

import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import forms.account.PremiumForm
import models.Plan

@Singleton
class PlansController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc) {

    import profile.api._

    def index = UserAwareAction.async { implicit request =>
        for {
            currency <- clientCurrency
        } yield Ok(views.html.account.plans.index(request.identity, currency))
    }

    def upgrade(planCode: String) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        val currency = clientCurrency

        Plan.byCode.get(planCode) match {
            case Some(plan) => {
                for {
                    currency <- clientCurrency
                    session <- paymentService.createSubscriptionSession(
                        user, plan, currency, Some(14),
                        routes.IndexController.index, routes.IndexController.index)
                } yield Ok(play.twirl.api.Html(f"<a href='${session.getUrl}'>Checkout</a>"))
            }
            case None => {
                Future.successful(
                    Redirect(routes.PlansController.index).
                        flashing("error" -> "This plan does not exist"))
            }
        }
    }

    def upgradeSubmit = SecuredAction.async { implicit request =>

        PremiumForm.form.bindFromRequest.fold(
            form => Future.successful(
                BadRequest(views.html.account.premium.upgrade(request.identity, form))),
            data => {
                val user = request.identity.user

                db.run(daos.studio.query.filter(_.ownerId === user.id).result).
                    flatMap { studios => emailService.sendPremiumRequest(user, data, studios) }.
                    map { _ =>
                        Redirect(routes.IndexController.index).
                            flashing("success" ->
                                ("Your Premium access request has been received, we will get " +
                                 "back to you shortly."))
                    }
            }
        )
    }
}
