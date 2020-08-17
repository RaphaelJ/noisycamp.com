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

package misc

import collection.JavaConverters._
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future, blocking }
import scala.math.BigDecimal.RoundingMode

import akka.util.ByteString
import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.checkout.Session
import com.stripe.model.oauth.TokenResponse
import com.stripe.model.{ Event, LoginLink, PaymentIntent }
import com.stripe.net.{ OAuth, RequestOptions, Webhook }
import play.api.Configuration
import play.api.mvc.{ Call, Request, RequestHeader, Result, Results }
import play.filters.csrf.CSRF
import squants.market.Money


import i18n.Currency
import models.{ Picture, Studio, User }

object PaymentCaptureMethod extends Enumeration {
    val Automatic = Value

    /** Funds will only be authorized but not captured immediately. */
    val Manual = Value
}

@Singleton
class PaymentService @Inject() (
    implicit val executionContext: TaskExecutionContext) {

    def secretKey(implicit config: Configuration): String = config.get[String]("stripe.secretKey")

    def webhookSecret(implicit config: Configuration): String =
        config.get[String]("stripe.webhookSecret")

    def requestOptions(implicit config: Configuration): RequestOptions = {
        RequestOptions.builder().
            setApiKey(secretKey).
            build
    }

    /** Initiate a Stripe Checkout transaction. */
    def initiatePayment(
        customer: User, amount: Money, title: String, description: String,
        statement: String, pics: Seq[Picture#Id],
        captureMethod: PaymentCaptureMethod.Value, onSuccess: Call, onCancel: Call)(
        implicit request: RequestHeader, config: Configuration): Future[Session] = {

        require(statement.length <= 22)

        val (stripeAmount, stripeCurrency) = PaymentService.asStripeAmount(amount)

        val picUrls: java.util.List[String] = pics.
            take(8).
            map(_.base64).
            map { id => controllers.routes.PictureController.cover(id, "500x500") }.
            map(_.absoluteURL).
            asJava

        val items: java.util.List[java.util.Map[String, AnyRef]] = Seq(
            Map(
                "name" -> title,
                "description" -> description,
                "amount" -> stripeAmount.asInstanceOf[AnyRef],
                "currency" -> stripeCurrency,
                "quantity" -> 1.asInstanceOf[AnyRef],
                "images" -> picUrls).asJava
            ).asJava

        val paymentMethods =
            if (
                amount.currency == Currency.EUR
                && captureMethod == PaymentCaptureMethod.Automatic
            ) {
                Seq("card", "ideal") // iDEAL only supports some transactions
            } else {
                Seq("card")
            }

        val paymentIntent: java.util.Map[String, Object] = Map(
            "capture_method" -> {
                captureMethod match {
                    case PaymentCaptureMethod.Automatic => "automatic"
                    case PaymentCaptureMethod.Manual => "manual"
                }
            }.asInstanceOf[Object],
            "description" -> description,
            "statement_descriptor" -> statement).asJava

        val params: java.util.Map[String, Object] = Map(
            "client_reference_id" -> customer.id.toString,
            "customer_email" -> customer.email,

            "success_url" -> onSuccess.absoluteURL,
            "cancel_url" -> onCancel.absoluteURL,
            "mode" -> "payment",
            "submit_type" -> "book",
            "payment_method_types" -> paymentMethods.asJava,
            "payment_intent_data" -> paymentIntent,

            "line_items" -> items).asJava

        Future { blocking { Session.create(params, requestOptions) } }
    }

    def retreiveSession(sessionId: String)(implicit config: Configuration): Future[Session] = {

        Future { blocking { Session.retrieve(sessionId, requestOptions) } }
    }

    def retreiveIntent(intentId: String)(implicit config: Configuration):
        Future[PaymentIntent] = {

        Future { blocking { PaymentIntent.retrieve(intentId, requestOptions) } }
    }

    def capturePayment(intent: PaymentIntent)(implicit config: Configuration):
        Future[PaymentIntent] = {

        Future { blocking { intent.capture(requestOptions) } }
    }

    def cancelPayment(intent: PaymentIntent)(implicit config: Configuration):
        Future[PaymentIntent] = {

        Future { blocking { intent.cancel(requestOptions) } }
    }

    /** Returns the URL to the Stripe Express onboarding for the provided user.
     */
    def connectOAuthUrl(user: User, redirectUrl: String)(
        implicit request: RequestHeader, config: Configuration): String = {

        val clientID = config.get[String]("stripe.clientID")
        val csrfToken = CSRF.getToken.get

        "https://connect.stripe.com/express/oauth/authorize" +
            "?redirect_uri=" + redirectUrl +
            "&client_id=" + clientID +
            "&state=" + csrfToken.value +
            "&stripe_user[email]=" + user.email +
            user.firstName.map("&stripe_user[first_name]=" + _).getOrElse("") +
            user.lastName.map("&stripe_user[last_name]=" + _).getOrElse("")
    }

    /** Completes the Stripe Connect OAuth process.
     *
     * @param code is the received Stripe OAuth code.
     *
     * @return the Stripe Connect response containing the account ID.
     */
    def connectOAuthComplete(code: String)(implicit config: Configuration):
        Future[TokenResponse] = {

        val params: java.util.Map[String, Object] = Map(
          "grant_type" -> "authorization_code",
          "code" -> code,
          "assert_capabilities" -> Seq("transfers").asJava).asJava

        Future { blocking { OAuth.token(params, requestOptions) } }
    }

    /** Returns the URL to the Stripe Express dashboard associated witht the
     * account. */
    def connectDashboardUrl(stripeUserId: String)(implicit config: Configuration):
        Future[String] = {

        val params: java.util.Map[String, Object] = Map.empty.asJava

        Future { blocking { LoginLink.createOnAccount(stripeUserId, params, requestOptions) } }.
            map { _.getUrl }
    }


    def withWebhookEvent(request: Request[ByteString], handler: Event => Future[Result])(
        implicit config: Configuration):
        Future[Result] = {

        val payload = request.body.decodeString("UTF-8")
        val signature = request.headers("Stripe-Signature")

        try {
            val event = Webhook.constructEvent(payload, signature, webhookSecret)
            handler(event)
        } catch {
            case e: Exception => Future.successful(Results.BadRequest(e.toString))
        }
    }
}

object PaymentService {
    /** Constructs a map with two amount and currency codew string values that
     * represents the provided amount as a Stripe API value. */
    def asStripeAmount(value: Money): (Long, String) = {
        val decimals = value.currency.formatDecimals
        val rounded = value.rounded(decimals, RoundingMode.DOWN)
        val amount = (rounded.amount * BigDecimal(10).pow(decimals)).toLong
        val code = rounded.currency.code

        (amount, code)
    }
}
