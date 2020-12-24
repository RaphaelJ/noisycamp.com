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

package controllers.account

import javax.inject._
import java.time.{ Instant, LocalDateTime, ZoneId }

import play.api._
import play.api.mvc._

import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import daos.CustomColumnTypes
import models.StudioBooking

@Singleton
class IndexController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc)
    with CustomColumnTypes {

    import profile.api._

    def index = SecuredAction.async { implicit request =>

        val user = request.identity.user

        val now = Instant.now
        
        // Pre-filters all bookings in the database as if they started in the latest possible
        // timezone. We will do the actual timezone-aware filtering on the backend side afterward.
        val minBeginsAt = LocalDateTime.ofInstant(now, ZoneId.of("UTC-12:00"))

        db.run({
            for {
                upcomingGuestBookings <- daos.studioBooking.activeBookings.
                    filter(_.customerId === user.id).
                    filter(_.beginsAt > minBeginsAt).
                    sortBy(_.beginsAt.desc).
                    join(daos.studio.query).on(_.studioId === _.id).
                    result

                upcomingStudioBookings <- daos.studioBooking.activeBookings.
                    filter(_.beginsAt > minBeginsAt).
                    sortBy(_.beginsAt.desc).
                    join(daos.studio.query.filter(_.ownerId === user.id)).on(_.studioId === _.id).
                    join(daos.user.query).on(_._1.customerId === _.id).
                    result

            } yield (upcomingGuestBookings, upcomingStudioBookings)
        }.transactionally).map { case (upcomingGuestBookings, upcomingStudioBookings) =>

            Ok(views.html.account.index(
                request.identity,
                upcomingGuestBookings.
                    filter { case (b, s) => b.isUpcoming(s, now) },
                upcomingStudioBookings.
                    map { case ((b, s), u) => (b, s, u) }.
                    filter { case (b, s, _) => b.isUpcoming(s, now) }))
        }
    }
}
