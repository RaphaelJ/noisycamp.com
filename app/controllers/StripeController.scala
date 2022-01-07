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

package controllers

import javax.inject._
import scala.concurrent.Future

import akka.util.ByteString
import com.stripe.model.{ checkout, Event }
import play.api.mvc._

import controllers.account.PlansController
import controllers.studios.BookingController
import daos.CustomColumnTypes
import misc.PaymentService
import models.StudioBooking

@Singleton
class StripeController @Inject() (
    ccc: CustomControllerCompoments,
    bookingController: BookingController,
    plansController: PlansController)
    extends CustomBaseController(ccc) {

    import profile.api._

    /** Receives a valid payment or subscription webhook notification from Stripe. */
    def webhook = Action(parse.byteString).async {
        implicit request: Request[ByteString] =>

        paymentService.withWebhookEvent(request, { event =>
            event.getType match {
                case "checkout.session.completed" => handleCheckoutSessionCompleted(event)
                case "customer.subscription.created" => handleSubscriptionCreated(event)
                case "customer.subscription.deleted" => handleSubscriptionDeleted(event)
                case "customer.subscription.updated" => handleSubscriptionUpdated(event)
                case "customer.subscription.trial_will_end" => handleSubscriptionTrialWillEnd(event)
                case _ => Future.successful(NotFound("event-type-unknown"))
            }
        })
    }

    private def handleCheckoutSessionCompleted(event: Event)(implicit request: RequestHeader):
        Future[Result] = {

        val session = event.getDataObjectDeserializer.getObject.
            get.
            asInstanceOf[checkout.Session]

        val chargeType = session.getMetadata.get("charge_type")

        chargeType match {
            case "booking" => {
                val onPaymentSuccess = (_: StudioBooking) => Ok("payment-success")
                val onPaymentFailure = (_: StudioBooking) => Ok("payment-failure")
                val onBookingNotFound = NotFound("booking-not-found")

                bookingController.handlePaymentCompleted(
                    Left(session), onPaymentSuccess, onPaymentFailure, onBookingNotFound)
            }
            case "plan" => plansController.handlePaymentCompleted(session)
            case _ => Future.successful(BadRequest("charge-type-unknown"))
        }
    }

    private def handleSubscriptionCreated(event: Event)(implicit request: RequestHeader):
        Future[Result] = {
        Future.successful(Ok(""))
    }

    private def handleSubscriptionDeleted(event: Event)(implicit request: RequestHeader):
        Future[Result] = {
        Future.successful(Ok(""))
    }

    private def handleSubscriptionUpdated(event: Event)(implicit request: RequestHeader):
        Future[Result] = {
        Future.successful(Ok(""))
    }

    private def handleSubscriptionTrialWillEnd(event: Event)(implicit request: RequestHeader):
        Future[Result] = {
        Future.successful(Ok(""))
    }
}
