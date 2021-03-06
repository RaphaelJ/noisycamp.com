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

import java.time.{ LocalDateTime, LocalDate, Instant, ZoneId }
import scala.concurrent.duration.Duration
import scala.math.BigDecimal.RoundingMode

import play.api.Configuration
import squants.market.{ Currency, Money }

import misc.URL

case class Studio(
    id:                 Studio#Id       = 0L,
    createdAt:          Instant         = Instant.now(),

    ownerId:            User#Id,

    published:          Boolean         = false,

    name:               String,
    description:        String,
    phone:              Option[String]  = None,

    usePractice:        Boolean,
    useRecording:       Boolean,
    useLive:            Boolean,
    useLessons:         Boolean,

    location:           Location,
    timezone:           ZoneId,
    openingSchedule:    OpeningSchedule,
    pricingPolicy:      PricingPolicy,
    bookingPolicy:      BookingPolicy,
    paymentPolicy:      PaymentPolicy) {

    type Id = Long

    def currency: Currency = location.address.country.currency

    def localPricingPolicy = LocalPricingPolicy(currency, pricingPolicy)

    def isOwner(user: User): Boolean = ownerId == user.id

    /** Returns true iff the studio is public or owned by the given user. */
    def canAccess(user: Option[User]): Boolean = published || (user.map(isOwner _).getOrElse(false))

    def canBeBooked(owner: User): Boolean = {
        (paymentPolicy.hasOnlinePayment && owner.isPayoutSetup) ||
        paymentPolicy.hasOnsitePayment
    }

    /** The current time at the studio's timezone. */
    def currentDateTime(now: Instant = Instant.now): LocalDateTime = {
        now.atZone(timezone).toLocalDateTime
    }

    /** The maximum (exclusive) date for which a booking can be done. */
    def maxBookingDate(now: Instant = Instant.now)(implicit config: Configuration): LocalDate = {
        currentDateTime(now).
            toLocalDate.
            plusDays(config.get[Duration]("noisycamp.maxBookingAdvance").toDays)
    }

    /** Returns a string of the ID and the URL encoded name of the studio. */
    def URLId: String = {
        val city = location.address.city
        val region =
            location.address.stateCode match {
                case Some(stateCode) => location.address.country.states(stateCode)
                case None => location.address.country.name
            }

        val title = s"${city} ${region} ${name}"
        s"${id}-${URL.titleAsURL(title)}"
    }

    /** Returns a string describing the type of studio. */
    def describeType: String = {
        (usePractice, useRecording, useLive) match {
            case (true, false, false) => "Rehearsal space"
            case (false, true, false) => "Recording studio"
            case (false, false, true) => "Live music space"
            case _ => "Music studio"
        }
    }
}
