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

package forms.components

import play.api.data.Form
import play.api.data.Forms._

import models.{ PaymentPolicy, PayoutMethod }

object PaymentPolicyForm {
  case class Data(
    onlinePayment: Option[PayoutMethodForm.Data],
    onsitePayment: Boolean)

  val form = Form {
    type TupleType = (Boolean, Option[PayoutMethodForm.Data], Boolean)

    tuple(
      "has-online-payment"  -> boolean,
      "payout-method"       -> optional(PayoutMethodForm.form.mapping),
      "has-onsite-payment"  -> boolean
    ).
      verifying("Payout method required.", {
        case (hasOnlinePayment, payoutMethod, _) =>
          !hasOnlinePayment || payoutMethod.isDefined
      }: (TupleType) => Boolean).
      transform(
        {
          case (_, payoutMethod, hasOnsitePayment) =>
            Data(payoutMethod, hasOnsitePayment)
        },
        {
          case policy => (policy.onlinePayment.isDefined, policy.onlinePayment,
            policy.onsitePayment)
        }: Data => TupleType
      )
  }
}
