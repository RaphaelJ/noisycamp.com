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

package controllers.studios

import javax.inject._
import java.time.{ Duration, LocalDate, LocalTime }

import scala.concurrent.Future

import play.api._
import play.api.mvc._

import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import daos.CustomColumnTypes
import models.{ BookingTimes, Studio }

@Singleton
class Booking @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc)
  with CustomColumnTypes {

  import profile.api._

  def show(id: Studio#Id, date: String, time: String, duration: Int)
    = silhouette.SecuredAction.async { implicit request =>

    // Parses string arguments
    // TODO: handle errors.
    val bookingTimes = BookingTimes(
      date = LocalDate.parse(date),
      time = LocalTime.parse(time),
      duration = Duration.ofSeconds(duration.toLong))

    for {
      clientConfig <- getClientConfig
      dbStudio <- db.run { daos.studioPicture.getStudioWithPictures(id) }
    } yield dbStudio match {
      case (Some(studio), picIds) => {
        val pricingPolicy = studio.
          localPricingPolicy.
          in(clientConfig.currency)(exchangeRatesService.exchangeRates)



        Ok(
          views.html.studios.book(
            clientConfig = clientConfig,
            user = Some(request.identity),
            studio, pricingPolicy, picIds, bookingTimes))
      }
      case (None, _) => NotFound("Studio not found.")
    }
  }
}
