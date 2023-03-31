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

package forms.account

import java.time.{ Instant, ZoneId }

import play.api.data.Form
import play.api.data.Forms._

import forms.CustomFields
import forms.components.PaymentPolicyForm
import i18n.TimeZoneService
import models.{
  BookingPolicy, Equipment, Location, OpeningSchedule, PricingPolicy, SerializedPicture, Studio,
  User }

/** A form to create and edit studios. */
object StudioForm {

    def form(canUseEquipmentPrice: Boolean) = Form(
        mapping(
            "general-info.name"              -> nonEmptyText,
            "general-info.description"       -> nonEmptyText,
            "general-info.phone"             -> CustomFields.optionalText,

            "general-info.use-practice"      -> boolean,
            "general-info.use-recording"     -> boolean,
            "general-info.use-live"          -> boolean,
            "general-info.use-lessons"       -> boolean,

            "location"              -> forms.components.LocationForm.form.mapping,
            "opening-schedule"      -> forms.components.OpeningScheduleForm.form.mapping,
            "pricing-policy"        -> forms.components.PricingPolicyForm.form.mapping,
            "booking-policy"        -> forms.components.BookingPolicyForm.form.mapping,
            "payment-policy"        -> forms.components.PaymentPolicyForm.form.mapping,

            "equipments"            ->
                seq(forms.components.EquipmentForm.form(canUseEquipmentPrice).mapping),
            "pictures"              -> seq(CustomFields.pictureId)
        )(Data.apply)(Data.unapply))

    case class Data(
        name:               String,
        description:        String,
        phone:              Option[String],

        usePractice:        Boolean,
        useRecording:       Boolean,
        useLive:            Boolean,
        useLessons:         Boolean,

        location:           Location,
        openingSchedule:    OpeningSchedule,
        pricingPolicy:      PricingPolicy,
        bookingPolicy:      BookingPolicy,
        paymentPolicy:      PaymentPolicyForm.Data,

        equipments:         Seq[Equipment],
        pictures:           Seq[SerializedPicture#Id]) {

        /** Constructs a new studio object from the form data, either with default values or from
         * an existing studio object. */
        def toStudio(initialValue: Either[User#Id, Studio])(
            implicit timeZoneService: TimeZoneService)
            : (Studio, Seq[Equipment], Seq[SerializedPicture#Id]) = {

            val (id, createdAt, ownerId, published) = initialValue match {
                case Left(ownerId) => (0L, Instant.now(), ownerId, false)
                case Right(studio) => (
                    studio.id, studio.createdAt, studio.ownerId, studio.published)
            }

            val timezone: ZoneId = timeZoneService.
              query(location.coordinates.lat, location.coordinates.long).
              getOrElse(ZoneId.of("UTC"))

            val studio = Studio(
                id = id,
                createdAt = createdAt,
                ownerId = ownerId,
                published = published,

                name = name,
                description = description,
                phone = phone,

                usePractice = usePractice,
                useRecording = useRecording,
                useLive = useLive,
                useLessons = useLessons,

                location = location,
                timezone = timezone,
                openingSchedule = openingSchedule,
                pricingPolicy = pricingPolicy,
                bookingPolicy = bookingPolicy,
                paymentPolicy = paymentPolicy)

            (studio, equipments, pictures)
        }
    }

    def fromStudio(
        canUseEquipmentPrice: Boolean, studio: Studio, equipments: Seq[Equipment],
        pictures: Seq[SerializedPicture#Id])
        : Form[Data] = {

        form(canUseEquipmentPrice).fill(Data(
            name = studio.name,
            description = studio.description,
            phone = studio.phone,

            usePractice = studio.usePractice,
            useRecording = studio.useRecording,
            useLive = studio.useLive,
            useLessons = studio.useLessons,

            location = studio.location,
            openingSchedule = studio.openingSchedule,
            pricingPolicy = studio.pricingPolicy,
            bookingPolicy = studio.bookingPolicy,
            paymentPolicy = studio.paymentPolicy,

            equipments, pictures))
    }
}
