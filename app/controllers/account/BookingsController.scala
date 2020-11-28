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

package controllers.account

import javax.inject._

import scala.concurrent.Future

import play.api._
import play.api.mvc._

import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import models.StudioBooking

@Singleton
class BookingsController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc) {

    import profile.api._

    /** Lists the bookings of the current user. */
    def index = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run({
            daos.studioBooking.query.
                join(daos.studio.query).on(_.studioId === _.id).
                filter { case (booking, _) => booking.customerId === user.id }.
                result
        }).map { bookings =>
            Ok(views.html.account.bookings.index(request.identity, bookings))
        }
    }

    def show(id: StudioBooking#Id) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run({
            val dbBooking = daos.studioBooking.query.
                filter(_.id === id).
                result.headOption

            dbBooking.flatMap {
                case Some(booking) => {
                    for {
                        studio <- daos.studio.query.
                            filter(_.id === booking.studioId).
                            result.head

                        studioOwner <- daos.user.query.
                            filter(_.id === studio.ownerId).
                            result.head

                    } yield Some((studio, studioOwner, booking))
                }
                case None => DBIO.successful(None)
            }
        }.transactionally).
            map {
                case Some((studio, studioOwner, booking)) if booking.isCustomer(user) => Ok(
                    views.html.account.bookings.show(
                        request.identity, studio, studioOwner, booking))
                case Some(_) => Forbidden("Cannot access other customers' bookings.")
                case None => NotFound("Booking not found.")
            }

    }
}
