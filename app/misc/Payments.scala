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
import scala.math.BigDecimal.RoundingMode

import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.checkout.Session
import com.stripe.net.RequestOptions
import play.api.Configuration
import play.api.mvc.{ Call, RequestHeader }
import squants.market.Money

import models.{ Studio, User }

object Payments {

  def secretKey(implicit config: Configuration): String =
    config.get[String]("mapbox.token")

  def requestOptions(implicit config: Configuration): RequestOptions = {
    RequestOptions.builder().
      setApiKey(secretKey).
      build
  }

  /** Constructs a map with two amount and currency codew string values that
   * represents the provided amount as a Stripe API value. */
  def asStripeAmount(value: Money): (Long, String) = {
    val decimals = value.currency.formatDecimals
    val rounded = value.rounded(decimals, RoundingMode.DOWN)
    val amount = (rounded.amount * BigDecimal(10).pow(decimals)).toLong
    val code = rounded.currency.code

    (amount, code)
  }

  /** Initiate a Stripe Checkout transaction. */
  def initiatePayment(customer: User, studio: Studio, amount: Money,
    onSuccess: Call, onCancel: Call)(
    implicit request: RequestHeader, config: Configuration): Session = {

    val title: String = f"Booking for '${studio.name}%s'"

    val (stripeAmount, stripeCurrency) = asStripeAmount(amount)
    val items: java.util.List[java.util.Map[String, AnyRef]] = Seq(
      Map(
        "name" -> title,
        "amount" -> stripeAmount.asInstanceOf[AnyRef],
        "currency" -> stripeCurrency,
        "quantity" -> 1.asInstanceOf[AnyRef]).asJava
      ).asJava

    val params: java.util.Map[String, Object] = Map(
      "customer" -> customer.id.toString,
      "customer_email" -> customer.email,

      "success_url" -> onSuccess.absoluteURL,
      "cancel_url" -> onCancel.absoluteURL,
      "mode" -> "payment",
      "payment_method_types" -> Seq("card", "ideal").asJava,

      "line-items" -> items).asJava

    Session.create(params, requestOptions)
  }
}
