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

import play.api.libs.json.{ JsNull, JsObject, Json, JsString, JsValue, Writes }
import play.api.mvc.RequestHeader
import squants.market.Money

import models.{
    Address, BookingPolicy, BookingTimes, CancellationPolicy, Coordinates, Event, LocalEquipment,
    LocalPricingPolicy, LocalEveningPricingPolicy, LocalWeekendPricingPolicy, Location,
    OpeningSchedule, OpeningTimes, PaymentPolicy, PictureId, Studio, StudioWithPicture,
    StudioWithPictureAndEquipments }

/** Provides JSON Writes implementation for model objects. */
object JsonWrites {

    implicit object AddressWrites extends Writes[Address] {
        def writes(address: Address): JsValue = Json.obj(
            "address1"      -> address.address1,
            "address2"      -> address.address2,
            "zipcode"       -> address.zipcode,
            "city"          -> address.city,
            "state-code"    -> address.stateCode,
            "country-code"  -> address.country.isoCode)
    }

    implicit object CancellationPolicyWrites extends Writes[CancellationPolicy] {
        def writes(policy: CancellationPolicy): JsValue = Json.obj(
            "notice"  -> policy.notice.getSeconds)
    }

    implicit object BookingPolicyWrites extends Writes[BookingPolicy] {
        def writes(policy: BookingPolicy): JsValue = Json.obj(
            "min-booking-duration"  -> policy.minBookingDuration.getSeconds,
            "automatic-approval"    -> policy.automaticApproval,
            "cancellation-policy"   -> policy.cancellationPolicy)
    }

    implicit object BookingTimesWrites extends Writes[BookingTimes] {
        def writes(times: BookingTimes): JsValue = Json.obj(
            "begins-at" -> times.beginsAt.toString,
            "duration" -> times.duration.getSeconds
        )
    }

    implicit object CoordinatesWrites extends Writes[Coordinates] {
        def writes(coords: Coordinates): JsValue = Json.obj(
            "long"  -> coords.long,
            "lat"   -> coords.lat)
    }

    implicit object EventWrites extends Writes[Event] {
        def writes(event: Event): JsValue = Json.obj(
            "begins-at"         -> event.beginsAt,
            "duration"          -> event.duration,

            "title"             -> event.title,
            "href"              -> event.href.map(_.url),

            "classes"           -> event.classes)
    }

    implicit object MoneyWrites extends Writes[Money] {
        def writes(amount: Money): JsValue = Json.obj(
            "currency" -> amount.currency.code,
            "value" -> amount.amount.toString)
    }

    implicit object LocalEveningPricingPolicyWrites extends Writes[LocalEveningPricingPolicy] {

        def writes(policy: LocalEveningPricingPolicy): JsValue = Json.obj(
            "begins-at"       -> policy.beginsAt,
            "price-per-hour"  -> policy.pricePerHour)
    }

    implicit object LocalEquipmentWrites extends Writes[LocalEquipment] {
        def writes(equip: LocalEquipment): JsValue = Json.obj(
            "id"              -> equip.id,
            "category"        -> equip.category.map(_.code),
            "details"         -> equip.details,
            "price-per-hour"  -> equip.pricePerHour)
    }

    implicit object LocalWeekendPricingPolicyWrites extends Writes[LocalWeekendPricingPolicy] {

        def writes(policy: LocalWeekendPricingPolicy): JsValue = Json.obj(
            "price-per-hour" -> policy.pricePerHour)
    }

    implicit object LocalPricingPolicyWrites extends Writes[LocalPricingPolicy] {
        def writes(policy: LocalPricingPolicy): JsValue = Json.obj(
            "price-per-hour" -> policy.pricePerHour,
            "evening" -> policy.evening,
            "weekend" -> policy.weekend)
    }

    implicit object LocationWrites extends Writes[Location] {
        def writes(location: Location): JsValue = Json.obj(
            "address"     -> location.address,
            "coordinates" -> location.coordinates)
    }

    implicit object OpeningScheduleWrites extends Writes[OpeningSchedule] {
        def writes(schedule: OpeningSchedule): JsValue = {
            def timeToJsValue(times: Option[OpeningTimes]) = {
                times match {
                    case Some(OpeningTimes(opensAt, closesAt)) => {
                        Json.obj(
                            "is-open" -> true,
                            "opens-at" -> opensAt,
                            "closes-at" -> closesAt)
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

    implicit object PaymentPolicyWrites extends Writes[PaymentPolicy] {
        def writes(policy: PaymentPolicy): JsValue = Json.obj(
            "has-online-payment"  -> policy.hasOnlinePayment,
            "has-onsite-payment"  -> policy.hasOnsitePayment)
    }

    implicit object PictureIdWrites extends Writes[PictureId] {
        def writes(id: PictureId): JsValue = JsString(id.base64)
    }

    implicit object StudioWrites extends Writes[Studio] {
        def writes(studio: Studio): JsValue = Json.obj(
            "id"                -> studio.id,
            "name"              -> studio.name,
            "description"       -> studio.description.take(255),
            "phone"             -> studio.phone,

            "location"          -> studio.location,
            "timezone"          -> studio.timezone.toString,
            "opening-schedule"  -> studio.openingSchedule,
            "pricing-policy"    -> studio.localPricingPolicy,
            "booking-policy"    -> studio.bookingPolicy,
            "payment-policy"    -> studio.paymentPolicy)
    }

    implicit object StudioWithPictureWrites extends Writes[StudioWithPicture] {
        def writes(value: StudioWithPicture): JsValue = {
            val studioObj = Json.toJson(value.studio).asInstanceOf[JsObject]
            val studioPic = Json.toJson(value.picture)

            studioObj ++ Json.obj("picture" -> studioPic)
        }
    }

    implicit object StudioWithPictureAndEquipmentsWrites
        extends Writes[StudioWithPictureAndEquipments] {

        def writes(value: StudioWithPictureAndEquipments): JsValue = {
            val studioObj = Json.toJson(value.studio).asInstanceOf[JsObject]
            val studioPic = Json.toJson(value.picture)
            val studioEquip = Json.toJson(value.equipments)

            studioObj ++ Json.obj(
                "picture"     -> studioPic,
                "equipments"  -> studioEquip)
        }
    }
}
