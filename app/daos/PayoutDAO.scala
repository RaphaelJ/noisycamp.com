/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphaeljavaux@gmail.com>
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

import squants.market

import models.{ Payout, User }

class PayoutDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class PayoutTable(tag: Tag)
    extends Table[Payout](tag, "payout") {

    def id              = column[Payout#Id]("id", O.PrimaryKey, O.AutoInc)
    def createdAt       = column[Instant]("created_at")

    def customerId      = column[User#Id]("customer_id")

    def stripePayoutId  = column[String]("stripe_payout_id")

    def currency        = column[market.Currency]("currency")
    def amount          = column[BigDecimal]("amount")

    private type PayoutTuple = (Payout#Id, Instant, User#Id, String,
      market.Currency, BigDecimal)

    private def toPayout(payoutTuple: PayoutTuple) = {
      val currency = payoutTuple._5

      Payout(payoutTuple._1, payoutTuple._2, payoutTuple._3, payoutTuple._4,
        currency(payoutTuple._6))
    }

    private def fromPayout(payout: Payout) = {
      Some((payout.id, payout.createdAt,
        payout.customerId,
        payout.stripePayoutId,
        payout.amount.currency, payout.amount.amount))
    }

    def * = (id, createdAt, customerId, stripePayoutId, currency, amount) <>
      (toPayout, fromPayout)
  }

  lazy val query = TableQuery[PayoutTable]

  lazy val insert = query returning
    query.map(_.id) into ((payout, id) => payout.copy(id=id))
}
