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

package test.misc

import org.scalatest.Matchers._
import org.scalatestplus.play._

import i18n.Currency
import misc.Payments

class PaymentsSpec extends PlaySpec {

  "Payments.asStripeAmount" must {
    "returns the currency amount in the smallest unit of the currency" in {
      Payments.asStripeAmount(Currency.CHF(233.2)) should be ((23320, "CHF"))

      Payments.asStripeAmount(Currency.EUR(12.542)) should be ((1254, "EUR"))

      Payments.asStripeAmount(Currency.ISK(450)) should be ((450, "ISK"))

      Payments.asStripeAmount(Currency.ISK(3072)) should be ((450, "ISK"))
    }
  }
}
