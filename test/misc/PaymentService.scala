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

package test.misc

import collection.JavaConverters._
import java.time.Duration
import javax.inject._
import scala.concurrent.{ Await, Future, duration }
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.Matchers._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.test.FakeRequest
import squants.market

import i18n.{ Country, Currency }
import misc.{ EquipmentCategory, PaymentService, StripeAccountType, StripePaymentCaptureMethod }
import models.{
    BookingDurations, LocalEquipmentPricePerHour, LocalEquipmentPricePerSession, Plan,
    PriceBreakdown, User }
import views.html.tags.priceBreakdown
import controllers.routes
import misc.EquipmentCategory
import org.scalatest.selenium.WebBrowser
import org.apache.xpath.operations.Bool

class PaymentServiceSpec extends PlaySpec with GuiceOneAppPerSuite {

    implicit val configuration = app.injector.instanceOf(classOf[Configuration])
    implicit val request = FakeRequest()
    val paymentService = app.injector.instanceOf(classOf[PaymentService])

    val timeout = duration.Duration(1, duration.MINUTES)

    // Tries the whole payment flow for a booking with all the supported currencies.

    "PaymenService" must {
        val customer = User(
            id = 1,
            firstName = Some("Alice"), lastName = Some("Testwoman"),
            email = "alice@testwoman.com", avatarId = None)

        val owner = {
            val user = User(
                id = 2,
                firstName = Some("Bob"), lastName = Some("Testman"),
                email = "bob@testman.com", avatarId = None)

            val country = Country.Belgium

            val address = Map(
                "line1" -> "address_full_match​",
                "postal_code" -> "4000",
                "city" -> "Liège",
                "country" -> country.isoCode
            ).asJava

            val extraParams = Map(
                "business_type" -> "individual",
                "individual" -> Map(
                    "first_name" -> "Bob",
                    "last_name" -> "Testman",
                    "dob" -> Map(
                        "day" -> "01".asInstanceOf[AnyRef],
                        "month" -> "01",
                        "year" -> "1902"
                    ).asJava,
                    "address" -> address,
                    "email" -> "bob@testman.com",
                    "phone" -> "+32412345678",
                    "id_number" -> "222222222",
                    "verification" -> Map(
                        "document" -> Map(
                            "front" -> "file_identity_document_success",
                        ).asJava,
                        "additional_document" -> Map(
                            "front" -> "file_identity_document_success",
                        ).asJava,
                    ).asJava,
                ).asJava,
                "external_account" -> Map(
                    "object" -> "bank_account",
                    "country" -> country.isoCode,
                    "currency" -> country.currency.code,
                    "account_number" -> "BE89370400440532013000",
                ).asJava,
                "company" -> Map(
                    "name" -> "Bob Testman",
                    "address" -> address,
                    "tax_id" -> "222222222",
                ).asJava,
                "business_profile" -> Map(
                    "mcc" -> 5734.asInstanceOf[AnyRef],
                    "url" -> "https://noisycamp.com",
                ).asJava,
                "tos_acceptance" -> Map(
                    "date" -> 1607130427.asInstanceOf[Object],
                    "ip" -> "83.134.216.114").asJava)

            paymentService.
                createAccount(user, country, StripeAccountType.Custom, extraParams).
                map { account =>
                    Thread.sleep(15000) // Stripe's account verification takes a few seconds.
                    user.copy(stripeAccountId = Some(account.getId))
                }
        }

        val currencies = Country.values.map(_.asInstanceOf[Country.Val].currency)

        for (currency <- currencies) {
            (f"support payments and refunds in $currency") in {
                val description = f"Booking flow test ${currency.name}"
                val statement = currency.name

                val equipmentPrices = Seq(
                    LocalEquipmentPricePerHour(currency(10)),
                    LocalEquipmentPricePerSession(currency(15)))

                val priceBreakdown = PriceBreakdown(
                    BookingDurations(Duration.ofHours(2), Duration.ZERO, Duration.ZERO),
                    currency(100), None, None, equipmentPrices, Some(Plan.Free.transactionRate))

                val intent = Await.result({
                    for {
                        owner <- owner
                        intent <- paymentService.createPaymentIntent(
                            owner, priceBreakdown, description, statement,
                            StripePaymentCaptureMethod.Manual
                        )
                        intent <- paymentService.retrievePaymentIntent(intent.getId)
                        intent <- paymentService.confirmPaymentIntent(
                            intent, Some("pm_card_mastercard_prepaid"))
                        intent <- paymentService.capturePaymentIntent(intent)
                    } yield intent
                }, timeout)

                intent.getStatus must be ("succeeded")

                val toBeCharged = PaymentService.asStripeAmount(priceBreakdown.total)._1
                val toBeTransfered = PaymentService.asStripeAmount(priceBreakdown.netTotal)._1

                intent.getAmount must be (toBeCharged)
                intent.getAmountCapturable must be (0)
                intent.getAmountReceived must be (toBeCharged)

                intent.getTransferData.getAmount must be (toBeTransfered)

                val refund = Await.result({
                    paymentService.refundPaymentIntent(intent.getId)
                }, timeout)

                refund.getAmount should be (toBeCharged)
            }
        }
    }

    "PaymentService.retrievePrice" must {
        // Checks that there exists a matching Stripe price for every plan and currency.

        for (plan <- Plan.values.filter(_.prices.isDefined)) {
            for (currency <- Currency.currencies) {
                (f"match ${plan.name} price for ${currency.code}") in {
                    val stripePrice = Await.result(
                        paymentService.retrievePrice(plan, currency),
                        timeout)

                    (stripePrice.isDefined) should be (true)

                    val planPrice = plan.prices.get.get(currency)

                    (planPrice.isDefined) should be (true)

                    val stripePriceValue = stripePrice.get

                    (stripePriceValue.getActive: Boolean) should be (true)

                    val (planAmount, planCurrency) = PaymentService.asStripeAmount(planPrice.get)

                    (stripePriceValue.getUnitAmount) should be (planAmount)
                    (stripePriceValue.getCurrency) should be (planCurrency)
                    (stripePriceValue.getBillingScheme) should be ("per_unit")

                    (stripePriceValue.getType) should be ("recurring")
                    (stripePriceValue.getRecurring.getInterval) should be ("month")
                }
            }
        }
    }

    "PaymentService.asStripeAmount" must {
        "returns the currency amount in the smallest unit of the currency" in {
            PaymentService.asStripeAmount(Currency.CHF(233.2)) should be ((23320, "chf"))
            PaymentService.asStripeAmount(Currency.EUR(12.542)) should be ((1254, "eur"))
            PaymentService.asStripeAmount(Currency.ISK(450)) should be ((45000, "isk"))
        }
    }

    "PaymentService.planPriceId" must {
        "generates the correct Stripe price ID" in {
            PaymentService.planPriceId(Plan.Free, Currency.CAD) should be (None)
            PaymentService.planPriceId(Plan.Premium, market.BTC) should be (None)
            PaymentService.planPriceId(Plan.Standard, Currency.CHF) should be
                (Some("noisycamp_standard_chf"))
        }
    }
}
