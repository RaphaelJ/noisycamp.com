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

package models

sealed trait PayoutMethod

object RecipientType extends Enumeration {
  val Business = Value
  val Private = Value
}

object AccountType extends Enumeration {
  val Checking = Value
  val Savings = Value
}

final case class Iban(
    val recipientType: RecipientType.Value, val bic: Option[String],
    val iban: String
  ) extends PayoutMethod

/** American Bankers Association routing number. */
final case class AbaAccount(
    val recipientType: RecipientType.Value, val routingNumber: String,
    val accountNumber: String, val accountType: AccountType.Value
  ) extends PayoutMethod

/** Canadian local bank account. */
final case class CanadianAccount(
    val recipientType: RecipientType.Value, val institutionNumber: String,
    val transitNumber: String, val accountNumber: String,
    val accountType: AccountType.Value
  ) extends PayoutMethod

/** Australian local bank account. */
final case class AustralianAccount(
    val recipientType: RecipientType.Value, val bsbCode: String,
    val businessNumber: Option[String], val accountNumber: String
  ) extends PayoutMethod

/** New Zealand bank account. */
final case class NewZealandAccount(
    val recipientType: RecipientType.Value, val accountNumber: String
  ) extends PayoutMethod
