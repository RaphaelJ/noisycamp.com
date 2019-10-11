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

package misc

import play.api.libs.json.{ Json, JsString, JsValue, Writes }

import models.{ OpeningSchedule, OpeningTimes, PictureId }

/** Provides JSON Writes implementation for model objects. */
object JsonWrites {
  implicit object OpeningScheduleWrites extends Writes[OpeningSchedule] {

    def writes(schedule: OpeningSchedule): JsValue = {

      def timeToJsValue(times: Option[OpeningTimes]) = {
        times match {
          case Some(OpeningTimes(opensAt, closesAt)) => {
            Json.obj(
              "is-open" -> true,
              "opens-at" -> opensAt,
              "closes-at" -> closesAt
            )
          }
          case None => Json.obj("is-open" -> false)
        }
      }

      Json.arr(
        timeToJsValue(schedule.monday), timeToJsValue(schedule.tuesday),
        timeToJsValue(schedule.wednesday), timeToJsValue(schedule.thursday),
        timeToJsValue(schedule.friday), timeToJsValue(schedule.saturday),
        timeToJsValue(schedule.sunday))
    }
  }

  implicit object PictureIdWrites extends Writes[PictureId] {
    def writes(id: PictureId): JsValue = JsString(id.base64)
  }
}
