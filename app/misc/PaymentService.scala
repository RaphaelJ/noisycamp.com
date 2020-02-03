/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>
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
import scala.concurrent.{ ExecutionContext, Future }
import scala.math.BigDecimal.RoundingMode

import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.checkout.Session
import com.stripe.model.PaymentIntent
import com.stripe.net.RequestOptions
import play.api.Configuration
import play.api.mvc.{ Call, RequestHeader }
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

  def secretKey(implicit config: Configuration): String =
    config.get[String]("stripe.secret_key")

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

    val (stripeAmount, stripeCurrency) = asStripeAmount(amount)

    val picUrls: java.util.List[String] = pics.
      take(8).
      map(_.base64).
      map { id => controllers.routes.PictureController.cover(id, "500x500") }.
      map(_.absoluteURL).
      asJava

    println(picUrls)

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

    println(onSuccess.absoluteURL)

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

    Future { Session.create(params, requestOptions) }
  }

  def retreiveSession(sessionId: String)(
    implicit request: RequestHeader, config: Configuration): Future[Session] = {

    Future { Session.retrieve(sessionId, requestOptions) }
  }

  def retreiveIntent(intentId: String)(
    implicit request: RequestHeader, config: Configuration):
    Future[PaymentIntent] = {

    Future { PaymentIntent.retrieve(intentId, requestOptions) }
  }

  def capturePayment(intent: PaymentIntent)(
    implicit request: RequestHeader, config: Configuration):
    Future[PaymentIntent] = {

    Future { intent.capture(requestOptions) }
  }

  def cancelPayment(intent: PaymentIntent)(
    implicit request: RequestHeader, config: Configuration):
    Future[PaymentIntent] = {

    Future { intent.cancel(requestOptions) }
  }

  /** Constructs a map with two amount and currency codew string values that
   * represents the provided amount as a Stripe API value. */
  private def asStripeAmount(value: Money): (Long, String) = {
    val decimals = value.currency.formatDecimals
    val rounded = value.rounded(decimals, RoundingMode.DOWN)
    val amount = (rounded.amount * BigDecimal(10).pow(decimals)).toLong
    val code = rounded.currency.code

    (amount, code)
  }
}
