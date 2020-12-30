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

package forms.studios

import scala.collection.mutable

import play.api.Configuration
import play.api.data.{ Form, Mapping }
import play.api.data.Forms._

import forms.CustomFields
import models.{ BookingTimes, Equipment, HasBookingTimes, PaymentMethod, Studio }

/** A form to place a booking request. */
object BookingForm {

    private val paymentMethod: Mapping[PaymentMethod.Value] =
        CustomFields.enumeration(Seq(
            PaymentMethod.Online  -> "online",
            PaymentMethod.Onsite  -> "onsite"))

    def form(studio: Studio, equipments: Seq[Equipment])(
        implicit config: Configuration): Form[Data] = {
        formGeneric(studio, equipments, optional(paymentMethod))
    }

    /** Same as `form` but forces the payment method to be defined. */
    def formWithPaymentMethod(studio: Studio, equipments: Seq[Equipment])(
        implicit config: Configuration): Form[DataWithPaymentMethod] = {
        formGeneric(studio, equipments, paymentMethod)
    }

    private def formGeneric[PM](
        studio: Studio, equipments: Seq[Equipment], paymentMethodField: Mapping[PM])(
        implicit config: Configuration): Form[DataGeneric[PM]] = {
            
        val equipmentsMap = equipments.map { e => e.id -> e }.toMap

        val equipmentField = longNumber.
            verifying("Invalid equipment", equipmentsMap.contains _).
            transform(equipmentsMap.apply _, (e: Equipment) => e.id)
            
        Form(
            mapping(
                "booking-times"     -> BookingTimesForm.form(studio).mapping,
                "equipments"        -> seq(equipmentField).
                    // Removes duplicates using a sorted set (O(n))
                    transform(
                        { es =>
                            val ids = mutable.Set.empty[Equipment#Id]
                            val uniqueEs = mutable.ArrayBuffer.empty[Equipment]
                            for (e <- es) {
                                if (!ids.contains(e.id)) {
                                    ids += e.id
                                    uniqueEs += e
                                }
                            }

                            uniqueEs.toSeq
                        },
                        (es: Seq[Equipment]) => es
                    ),
                "payment-method"    -> paymentMethodField,
            )(DataGeneric.apply)(DataGeneric.unapply))
    }

    case class DataGeneric[PM](
        bookingTimes:   BookingTimes,
        equipments:     Seq[Equipment],
        paymentMethod:  PM)
        extends HasBookingTimes

    type Data = DataGeneric[Option[PaymentMethod.Value]]
    type DataWithPaymentMethod = DataGeneric[PaymentMethod.Value]
}
