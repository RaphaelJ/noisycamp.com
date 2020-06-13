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

package models

import java.time.{ Instant, LocalDate, ZoneId }

import squants.market.Money

/** Stores the information about an user. */
case class Payout(
  id: Payout#Id = 0L,
  createdAt: Instant = Instant.now(),

  customerId: User#Id,

  stripePayoutId: String,

  amount: Money) {

  type Id = Long

  /** Returns the UTC date of the payout. */
  def date: LocalDate = LocalDate.ofInstant(createdAt, ZoneId.of("UTC"))
}
