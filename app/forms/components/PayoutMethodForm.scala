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

import play.api.data.{ Form, Mapping }
import play.api.data.format.Formatter
import play.api.data.Forms._

import i18n.Country
import models.{
  AccountType, AbaAccount, AustralianAccount, CanadianAccount, Iban,
  NewZealandAccount, PayoutAccount, RecipientType }
import forms.CustomFields

object PayoutMethodForm {

  case class Data (
    val country: Country.Val,
    val recipientType: RecipientType.Value,
    val recipientName: String,
    val account: PayoutAccount)

  private val recipientTypeMapping = CustomFields.enumeration(
    RecipientType.Business  -> "business",
    RecipientType.Private   -> "private")

  private val ibanMapping: Mapping[Iban] = mapping(
    "bic"   -> CustomFields.optionalText,
    "iban"  -> nonEmptyText)(Iban.apply)(Iban.unapply)

  private val accountType = CustomFields.enumeration(
    AccountType.Checking  -> "checking",
    AccountType.Savings   -> "savings")

  private val abaAccountMapping: Mapping[AbaAccount] = mapping(
    "address"         -> AddressForm.form.mapping,
    "routing-number"  -> nonEmptyText,
    "account-number"  -> nonEmptyText,
    "account-type"    -> accountType)(AbaAccount.apply)(AbaAccount.unapply)

  private val canadianAccountMapping: Mapping[CanadianAccount] = mapping(
    "institution-number"  -> nonEmptyText,
    "transit-number"      -> nonEmptyText,
    "account-number"      -> nonEmptyText,
    "account-type"        -> accountType
  )(CanadianAccount.apply)(CanadianAccount.unapply)

  private val australianAccountMapping: Mapping[AustralianAccount] = mapping(
    "address"         -> AddressForm.form.mapping,
    "bsb-code"        -> nonEmptyText,
    "business-number" -> CustomFields.optionalText,
    "account-number"  -> nonEmptyText
  )(AustralianAccount.apply)(AustralianAccount.unapply)

  private val newZealandAccountMapping: Mapping[NewZealandAccount] = mapping(
    "account-number"  -> nonEmptyText
  )(NewZealandAccount.apply)(NewZealandAccount.unapply)

  val form = Form {
    type TupleType = (Country.Val, RecipientType.Value, String, Option[Iban],
      Option[AbaAccount], Option[CanadianAccount], Option[AustralianAccount],
      Option[NewZealandAccount])

    tuple(
      "country" -> CustomFields.country,

      "recipient-type" -> recipientTypeMapping,
      "recipient-name" -> nonEmptyText,

      "iban" -> optional(ibanMapping),
      "aba-account" -> optional(abaAccountMapping),
      "canadian-account" -> optional(canadianAccountMapping),
      "australian-account" -> optional(australianAccountMapping),
      "new-zealand-account" -> optional(newZealandAccountMapping)
    ).
      verifying("Invalid account format", {
        case (country, _, _, iban, abaAccount, canadianAccount,
          australianAccount, newZealandAccount) =>

          country.accountType match {
            case i18n.AccountType.Iban => iban.isDefined
            case i18n.AccountType.AbaAccount => abaAccount.isDefined
            case i18n.AccountType.CanadianAccount => canadianAccount.isDefined
            case i18n.AccountType.AustralianAccount =>
              australianAccount.isDefined
            case i18n.AccountType.NewZealandAccount =>
              newZealandAccount.isDefined
            case _ => throw new Exception("Unknown account type.")
          }
      }: (TupleType) => Boolean).
      transform(
        {
          case (country, recipientType, recipientName, iban, abaAccount,
            canadianAccount, australianAccount, newZealandAccount) => {

            val account = country.accountType match {
              case i18n.AccountType.Iban => iban.get
              case i18n.AccountType.AbaAccount => abaAccount.get
              case i18n.AccountType.CanadianAccount => canadianAccount.get
              case i18n.AccountType.AustralianAccount => australianAccount.get
              case i18n.AccountType.NewZealandAccount => newZealandAccount.get
              case _ => throw new Exception("Unknown account type.")
            }

            Data(
              country, recipientType, recipientName, account)
          }
        },
        {
          case Data(country, recipientType, recipientName, account) =>

            val iban = if (account.isInstanceOf[Iban]) {
                Some(account.asInstanceOf[Iban])
              } else {
                None
              }

            val abaAccount = if (account.isInstanceOf[AbaAccount]) {
                Some(account.asInstanceOf[AbaAccount])
              } else {
                None
              }

            val canadianAccount = if (account.isInstanceOf[CanadianAccount]) {
                Some(account.asInstanceOf[CanadianAccount])
              } else {
                None
              }

            val australianAccount =
              if (account.isInstanceOf[AustralianAccount]) {
                Some(account.asInstanceOf[AustralianAccount])
              } else {
                None
              }
            val newZealandAccount =
              if (account.isInstanceOf[NewZealandAccount]) {
                Some(account.asInstanceOf[NewZealandAccount])
              } else {
                None
              }

            (country, recipientType, recipientName, iban, abaAccount,
              canadianAccount, australianAccount, newZealandAccount)
        }: Data => TupleType
      )
  }
}
