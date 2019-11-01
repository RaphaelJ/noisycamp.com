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

import play.api.libs.json.{ JsNull, Json, JsString, JsValue, Writes }
import squants.market.Money

import models.{
  LocalPricingPolicy, LocalEveningPricingPolicy, LocalWeekendPricingPolicy,
  OpeningSchedule, OpeningTimes, PictureId }

/** Provides JSON Writes implementation for model objects. */
object JsonWrites {

  implicit object MoneyWrites extends Writes[Money] {
    def writes(amount: Money): JsValue = Json.obj(
      "currency" -> amount.currency.code,
      "value" -> amount.amount.toString
    )
  }

  implicit object LocalEveningPricingPolicyWrites
    extends Writes[LocalEveningPricingPolicy] {

    def writes(policy: LocalEveningPricingPolicy): JsValue = Json.obj(
      "begins-at" -> policy.beginsAt,
      "price-per-hour" -> policy.pricePerHour
    )
  }

  implicit object LocalWeekendPricingPolicyWrites
    extends Writes[LocalWeekendPricingPolicy] {

    def writes(policy: LocalWeekendPricingPolicy): JsValue = Json.obj(
      "price-per-hour" -> policy.pricePerHour
    )
  }

  implicit object LocalPricingPolicyWrites extends Writes[LocalPricingPolicy] {
    def writes(policy: LocalPricingPolicy): JsValue = Json.obj(
      "price-per-hour" -> policy.pricePerHour,
      "evening" -> policy.evening,
      "weekend" -> policy.weekend
    )
  }

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
