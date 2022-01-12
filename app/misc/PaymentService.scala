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
import com.stripe.model.{
    Account, AccountLink, Event, File, LoginLink, PaymentIntent, Price, Refund, Subscription }
import com.stripe.net.{ OAuth, RequestOptions, Webhook }
import com.stripe.param.{ FileCreateParams, RefundCreateParams }
import play.api.Configuration
import play.api.mvc.{ Call, Request, RequestHeader, Result, Results }
import play.filters.csrf.CSRF
import squants.market

import i18n.{ Country, Currency }
import models.{ Picture, Plan, Studio, User }
import models.PriceBreakdown
import views.html.tags.priceBreakdown

object StripeAccountType extends Enumeration {
    val Express = Value
    val Custom = Value
}

object StripePaymentCaptureMethod extends Enumeration {
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

    def platformCurrency(implicit config: Configuration): market.Currency = {
        Currency.byCode(config.get[String]("stripe.platformCurrency"))
    }

    /** Creates a Stripe Express account for the provided NoisyCamp user. */
    def createAccount(
        user: User, country: Country.Val,
        accountType: StripeAccountType.Value = StripeAccountType.Express,
        extraParams: Map[String, Object] = Map.empty)(
        implicit config: Configuration):
        Future[Account] = {

        for {
            icon <- fileUpload(
                "public/images/app-icon-180.png", FileCreateParams.Purpose.BUSINESS_LOGO)

            params: java.util.Map[String, Object] = (Map(
                "country" -> country.isoCode,
                "type" -> (accountType match {
                    case StripeAccountType.Express => "express"
                    case StripeAccountType.Custom => "custom"
                }),
                "capabilities" -> Map(
                    "transfers" -> Map("requested" -> true.asInstanceOf[Object]).asJava,
                    "card_payments" -> Map("requested" -> true.asInstanceOf[Object]).asJava
                ).asJava,
                "email" -> user.email,
                "metadata" -> Map(
                    "noisycamp_id" -> user.id,
                ).asJava,
                "settings" -> Map(
                    "branding" -> Map(
                        "logo" -> icon.getId,
                        "primary_color" -> "#110903".asInstanceOf[Object],
                        "secondary_color" -> "#db7233"
                    ).asJava,
                    "payouts" -> Map(
                        "schedule" -> Map(
                            "interval" -> "manual".asInstanceOf[Object]
                        ).asJava
                    ).asJava
                ).asJava) ++
                extraParams).asJava

            account <- Future { blocking { Account.create(params, requestOptions) } }
        } yield account
    }

    def retrieveAccount(stripeAccountId: String)(
        implicit request: RequestHeader, config: Configuration): Future[Account] = {

        Future { blocking { Account.retrieve(stripeAccountId, requestOptions) } }
    }

    /** Returns the URL to the Stripe onboarding flow for the provided user. */
    def connectOAuthUrl(stripeAccountId: String, refreshUrl: String, returnUrl: String)(
        implicit config: Configuration):
        Future[String] = {

        val params: java.util.Map[String, Object] = Map[String, AnyRef](
            "account" -> stripeAccountId,
            "refresh_url" -> refreshUrl,
            "return_url" -> returnUrl,
            "type" -> "account_onboarding").asJava

        Future { blocking { AccountLink.create(params, requestOptions).getUrl } }
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
            "assert_capabilities" -> Seq("transfers", "card_payments").asJava).asJava

        Future { blocking { OAuth.token(params, requestOptions) } }
    }

    /** Returns the URL to the Stripe Express dashboard associated with the
     * account. */
    def connectDashboardUrl(stripeAccountId: String)(implicit config: Configuration):
        Future[String] = {

        val params: java.util.Map[String, Object] = Map.empty.asJava

        Future { blocking { LoginLink.createOnAccount(stripeAccountId, params, requestOptions) } }.
            map { _.getUrl }
    }

    /** Initiate a Stripe Checkout transaction for a booking. */
    def createSession(
        from: User, to: User, priceBreakdown: PriceBreakdown,
        title: String, description: String, statement: String, pics: Seq[Picture#Id],
        captureMethod: StripePaymentCaptureMethod.Value, onSuccess: Call, onCancel: Call,
        metadata: Map[String, String])(
        implicit request: RequestHeader, config: Configuration): Future[Session] = {

        require(statement.length <= 22)
        require(!to.stripeAccountId.isEmpty)

        val reference = f"booking_"

        val paymentMethods = Seq("card")

        val picUrls = pics.
            take(8).
            map(_.base64).
            map { id => controllers.routes.PictureController.cover(id, "500x500") }.
            map(_.absoluteURL)

        val amount = priceBreakdown.total
        val (stripeAmount, stripeCurrency) = PaymentService.asStripeAmount(amount)

        val transferAmount: Long = PaymentService.asStripeAmount(priceBreakdown.netTotal)._1

        val items: java.util.List[java.util.Map[String, AnyRef]] = Seq((
                Map(
                    "name" -> title,
                    "description" -> description,
                    "amount" -> stripeAmount.asInstanceOf[AnyRef],
                    "currency" -> stripeCurrency,
                    "quantity" -> 1.asInstanceOf[AnyRef]) ++ (
                if (picUrls.nonEmpty) { Map("images" -> picUrls.asJava) } else { Map.empty })
            ).asJava).asJava

        val params: java.util.Map[String, Object] = Map(
            "customer_email" -> from.email,

            "success_url" -> onSuccess.absoluteURL(true),
            "cancel_url" -> onCancel.absoluteURL(true),
            "mode" -> "payment",
            "submit_type" -> "book",
            "payment_method_types" -> paymentMethods.asJava,
            "payment_intent_data" -> paymentIntentParams(
                to, priceBreakdown, description: String, statement: String, captureMethod),
            "line_items" -> items,
            "metadata" -> metadata.asJava,
        ).asJava

        Future { blocking { Session.create(params, requestOptions) } }
    }

    def createSubscriptionSession(
        customer: User, plan: Plan.Val, currency: market.Currency, trialDays: Option[Int],
        onSuccess: Call, onCancel: Call, metadata: Map[String, String])(
        implicit request: RequestHeader, config: Configuration): Future[Session] = {

        require(plan.prices.isDefined)

        val paymentMethods =
            Seq("card") ++
            (if (currency == Currency.EUR) Seq("sepa_debit") else Seq.empty)

        val priceId = PaymentService.planPriceId(plan, currency).get

        val items: java.util.List[java.util.Map[String, AnyRef]] = Seq(
                Map(
                    "price"     -> priceId,
                    "quantity"  -> 1.asInstanceOf[AnyRef]
                ).asJava,
            ).asJava

        val params: java.util.Map[String, Object] = Map(
            "customer_email" -> customer.email,

            "success_url" -> onSuccess.absoluteURL(true),
            "cancel_url" -> onCancel.absoluteURL(true),

            "mode" -> "subscription",
            "payment_method_types" -> paymentMethods.asJava,
            "line_items" -> items,

            "allow_promotion_codes" -> true.asInstanceOf[AnyRef],

            "subscription_data" ->
                (trialDays match {
                    case Some(days) => Map("trial_period_days" -> 14.asInstanceOf[AnyRef])
                    case None => Map.empty
                }).asJava,

            "metadata" -> metadata.asJava,
        ).asJava

        Future { blocking { Session.create(params, requestOptions) } }
    }

    def retrieveSession(sessionId: String)(implicit config: Configuration): Future[Session] = {
        Future { blocking { Session.retrieve(sessionId, requestOptions) } }
    }

    /** Initiate a Stripe transaction without a Checkout session. */
    def createPaymentIntent(
        to: User, priceBreakdown: PriceBreakdown,
        description: String, statement: String, captureMethod: StripePaymentCaptureMethod.Value)(
        implicit request: RequestHeader, config: Configuration): Future[PaymentIntent] = {

        val amount = priceBreakdown.total
        val (stripeAmount, stripeCurrency) = PaymentService.asStripeAmount(amount)

        val extraParams = Map(
            "amount" -> stripeAmount.asInstanceOf[AnyRef],
            "currency" -> stripeCurrency,
        )

        val params = paymentIntentParams(
            to, priceBreakdown, description, statement, captureMethod, extraParams)
        Future { blocking { PaymentIntent.create(params, requestOptions) } }
    }

    def retrievePaymentIntent(intentId: String)(implicit config: Configuration):
        Future[PaymentIntent] = {

        Future { blocking { PaymentIntent.retrieve(intentId, requestOptions) } }
    }

    def confirmPaymentIntent(intent: PaymentIntent, paymentMethod: Option[String] = None)(
        implicit config: Configuration):
        Future[PaymentIntent] = {

        val params: java.util.Map[String, Object] =
            paymentMethod.
                map { m => Map("payment_method" -> m.asInstanceOf[AnyRef]) }.
                getOrElse(Map.empty).
                asJava

        Future { blocking { intent.confirm(params, requestOptions) } }
    }

    def capturePaymentIntent(intent: PaymentIntent)(implicit config: Configuration):
        Future[PaymentIntent] = {

        Future { blocking { intent.capture(requestOptions) } }
    }

    /** Cancels an uncaptured payment. */
    def cancelPaymentIntent(intent: PaymentIntent)(implicit config: Configuration):
        Future[PaymentIntent] = {

        Future { blocking { intent.cancel(requestOptions) } }
    }

    /** Refunds a captured payment. */
    def refundPaymentIntent(intentId: String, refundApplicationFee: Boolean = true)(
        implicit config: Configuration):
        Future[Refund] = {

        val params: java.util.Map[String, Object] = Map(
            "payment_intent" -> intentId,
            "reverse_transfer" -> true.asInstanceOf[AnyRef],
            "refund_application_fee" -> refundApplicationFee.asInstanceOf[AnyRef]).asJava

        Future { blocking { Refund.create(params, requestOptions) } }
    }

    def retrieveSubscription(subscriptionId: String)(implicit config: Configuration):
        Future[Subscription] = {

        Future { blocking { Subscription.retrieve(subscriptionId, requestOptions) } }
    }

    def cancelSubscription(subscription: Subscription)(implicit config: Configuration):
        Future[Subscription] = {

        val params = java.util.Map.of[String, Object]()
        Future { blocking { subscription.cancel(params, requestOptions) } }
    }

    def retrievePrice(plan: Plan.Value, currency: market.Currency)(implicit config: Configuration):
        Future[Option[Price]] = {

        PaymentService.planPriceId(plan, currency) match {
            case Some(priceId) => {
                Future { blocking { Some(Price.retrieve(priceId, requestOptions)) } }
            }
            case None => Future.successful(None)
        }
    }

    /** Creates a Stripe file by uploading the provided file. */
    def fileUpload(path: String, purpose: FileCreateParams.Purpose)(
        implicit config: Configuration): Future[File] = {

        val params = FileCreateParams.builder().
            setFile(new java.io.File(path)).
            setPurpose(purpose).
            build()

        Future { blocking { File.create(params, requestOptions) } }
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

    private def paymentIntentParams(
        to: User,
        priceBreakdown: PriceBreakdown,
        description: String, statement: String,
        captureMethod: StripePaymentCaptureMethod.Value,
        extraParams: Map[String, AnyRef] = Map.empty,
    )(implicit config: Configuration): java.util.Map[String, Object] = {

        require(statement.length <= 22)
        require(!to.stripeAccountId.isEmpty)

        val currency = priceBreakdown.pricePerHour.currency

        val transferAmount: Long = PaymentService.asStripeAmount(priceBreakdown.netTotal)._1

        // Uses the connected account as the processing account if the currency does not match the
        // platform's.
        val onBehalfOf =
            if (platformCurrency == currency) { Map.empty }
            else { Map("on_behalf_of" -> to.stripeAccountId.get) }

        (Map(
                "capture_method" -> {
                    captureMethod match {
                        case StripePaymentCaptureMethod.Automatic => "automatic"
                        case StripePaymentCaptureMethod.Manual => "manual"
                    }
                }.asInstanceOf[AnyRef],
                "statement_descriptor" -> statement,
                "description" -> description,
                "transfer_data" -> Map(
                    "destination" -> to.stripeAccountId.get,
                    "amount" -> transferAmount.asInstanceOf[AnyRef]).asJava
            ) ++
            onBehalfOf ++ extraParams
        ).asJava
    }
}

object PaymentService {

    /** Constructs a map with two amount and currency code string values that
     * represents the provided amount as a Stripe API value. */
    def asStripeAmount(value: market.Money): (Long, String) = {
        val decimals = value.currency.formatDecimals
        val rounded = value.rounded(decimals, RoundingMode.DOWN)
        val amount = (rounded.amount * BigDecimal(10).pow(decimals)).toLong
        val code = rounded.currency.code.toLowerCase

        (amount, code)
    }

    /** Returns an unique price ID that can be used to identify the plan price when billing the
     * customer with Stripe. */
    def planPriceId(plan: Plan.Value, currency: market.Currency): Option[String] = {
        plan.prices.
            flatMap { _.get(currency) }.
            map { _ => f"noisycamp_${plan.name.toLowerCase}_${currency.code.toLowerCase}" }
    }
}
