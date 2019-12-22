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

package daos

import scala.concurrent.ExecutionContext
import java.time.Instant
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import i18n.Country
import models.{ AbaAccount, AccountType, Address, AustralianAccount,
  CanadianAccount, Iban, PayoutMethod, RecipientType, NewZealandAccount, User }

class PayoutMethodDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
  )(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class PayoutMethodTable(tag: Tag)
    extends Table[PayoutMethod](tag, "payout_method") {

    def id                  =
      column[PayoutMethod#Id]("id", O.PrimaryKey, O.AutoInc)
    def createdAt           = column[Instant]("created_at")

    def ownerId             = column[User#Id]("owner_id")

    def country             = column[Country.Val]("country_code")

    def address1            = column[Option[String]]("address1")
    def address2            = column[Option[String]]("address2")
    def zipcode             = column[Option[String]]("zipcode")
    def city                = column[Option[String]]("city")
    def stateCode           = column[Option[String]]("state_code")

    def recipientType       = column[RecipientType.Value]("recipient_type")
    def recipientName       = column[String]("recipient_name")

    def accountType         = column[String]("account_type")

    def ibanBic             = column[Option[String]]("iban_bic")
    def ibanIban            = column[Option[String]]("iban_iban")

    def abaRoutingNumber    = column[Option[String]]("aba_routing_number")
    def abaAccountNumber    = column[Option[String]]("aba_account_number")
    def abaAccountType      =
      column[Option[AccountType.Value]]("aba_account_type")

    def canadianInstitutionNumber =
      column[Option[String]]("canadian_institution_number")
    def canadianTransitNumber =
      column[Option[String]]("canadian_transit_number")
    def canadianAccountNumber =
      column[Option[String]]("canadian_account_number")
    def canadianAccountType =
      column[Option[AccountType.Value]]("canadian_account_type")

    def australianBsbCode = column[Option[String]]("australian_bsb_code")
    def australianBusinessNumber =
      column[Option[String]]("australian_business_number")
    def australianAccountNumber =
      column[Option[String]]("australian_account_number")

    def newZealandAccountNumber =
      column[Option[String]]("new_zealand_account_number")

    private type PayoutMethodTuple = (
      PayoutMethod#Id, Instant, User#Id,
      Country.Val, AddressTuple,
      RecipientType.Value, String,
      String, IbanTuple, AbaTuple, CanadianTuple, AustralianTuple,
      NewZealandTuple)

    private type AddressTuple = (
      Option[String], Option[String], Option[String], Option[String],
      Option[String])

    private type IbanTuple = (Option[String], Option[String])

    private type AbaTuple = (Option[String], Option[String],
      Option[AccountType.Value])

    private type CanadianTuple = (Option[String], Option[String],
      Option[String], Option[AccountType.Value])

    private type AustralianTuple = (Option[String], Option[String],
      Option[String])

    private type NewZealandTuple = (Option[String])

    private val payoutMethodShaped = (
      id, createdAt, ownerId,
      country, (address1, address2, zipcode, city, stateCode),
      recipientType, recipientName,
      accountType,
      (ibanBic, ibanIban),
      (abaRoutingNumber, abaAccountNumber, abaAccountType),
      (canadianInstitutionNumber, canadianTransitNumber, canadianAccountNumber,
      canadianAccountType),
      (australianBsbCode, australianBusinessNumber, australianAccountNumber),
      (newZealandAccountNumber)
      ).shaped

    private def toAddress(country: Country.Val, addressTuple: AddressTuple) = {
      Address(addressTuple._1.get, addressTuple._2, addressTuple._3.get,
        addressTuple._4.get, addressTuple._5, country)
    }

    private def fromAddress(addressOpt: Option[Address]) = {
      addressOpt match {
        case Some(address) => (Some(address.address1), address.address2,
          Some(address.zipcode), Some(address.city), address.stateCode)
        case None => (None, None, None, None, None)
      }
    }

    private def toPayoutMethod(payoutTuple: PayoutMethodTuple) = {
      val account = payoutTuple._8 match {
        case "iban" => {
          val ibanTuple = payoutTuple._9
          Iban(ibanTuple._1, ibanTuple._2.get)
        }
        case "aba" => {
          val address = toAddress(payoutTuple._4, payoutTuple._5)
          val abaTuple = payoutTuple._10
          AbaAccount(address, abaTuple._1.get, abaTuple._2.get, abaTuple._3.get)
        }
        case "canadian" => {
          val canadianTuple = payoutTuple._11
          CanadianAccount(canadianTuple._1.get, canadianTuple._2.get,
            canadianTuple._3.get, canadianTuple._4.get)
        }
        case "australian" => {
          val address = toAddress(payoutTuple._4, payoutTuple._5)
          val australianTuple = payoutTuple._12
          AustralianAccount(address, australianTuple._1.get,
            australianTuple._2, australianTuple._3.get)
        }
        case "new-zealand" => {
          NewZealandAccount(payoutTuple._13.get)
        }
      }

      PayoutMethod(
        payoutTuple._1, payoutTuple._2, payoutTuple._3,
        payoutTuple._4, payoutTuple._6, payoutTuple._7,
        account)
    }

    private def fromPayoutMethod(payout: PayoutMethod) = {
      val (accountType, address) = payout.account match {
        case _: Iban => ("iban", None)
        case abaAccount: AbaAccount => ("aba",
          Some(abaAccount.recipientAddress))
        case _: CanadianAccount => ("canadian", None)
        case australianAccount: AustralianAccount => ("australian",
          Some(australianAccount.recipientAddress))
        case _: NewZealandAccount => ("new-zealand", None)
      }

      val ibanTuple = payout.account match {
        case ibanAccount: Iban => (ibanAccount.bic, Some(ibanAccount.iban))
        case _ => (None, None)
      }

      val abaTuple: AbaTuple = payout.account match {
        case abaAccount: AbaAccount => (Some(abaAccount.routingNumber),
          Some(abaAccount.accountNumber), Some(abaAccount.accountType))
        case _ => (None, None, None)
      }

      val canadianTuple = payout.account match {
        case canadianAccount: CanadianAccount => (
          Some(canadianAccount.institutionNumber),
          Some(canadianAccount.transitNumber),
          Some(canadianAccount.accountNumber),
          Some(canadianAccount.accountType))
        case _ => (None, None, None, None)
      }

      val australianTuple = payout.account match {
        case australianAccount: AustralianAccount => (
          Some(australianAccount.bsbCode), australianAccount.businessNumber,
          Some(australianAccount.accountNumber))
        case _ => (None, None, None)
      }

      val newZealandTuple = payout.account match {
        case newZealandAccount: NewZealandAccount => (
          Some(newZealandAccount.accountNumber))
        case _ => (None)
      }

      Some((payout.id, payout.createdAt, payout.ownerId,
        payout.country, fromAddress(address),
        payout.recipientType, payout.recipientName,
        accountType, ibanTuple, abaTuple, canadianTuple, australianTuple,
        newZealandTuple))
    }

    def * = payoutMethodShaped <> (toPayoutMethod, fromPayoutMethod)
  }

  lazy val query = TableQuery[PayoutMethodTable]

  /** Inserts a payout method and returns the newly created object with its
   * inserted ID. */
  def insert(method: PayoutMethod): DBIO[PayoutMethod] = {
    query returning query.map(_.id) into ((s, id) => s.copy(id=id)) += method
  }
}
