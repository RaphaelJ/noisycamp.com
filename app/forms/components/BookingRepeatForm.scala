/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2021  Raphael Javaux <raphael@noisycamp.com>
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

package forms.components

import java.time.LocalDate

import play.api.Configuration
import play.api.data.{ Form, Mapping }
import play.api.data.Forms._

import forms.CustomFields
import models.{
    BookingRepeat, BookingRepeatCount, BookingRepeatFrequency, BookingRepeatUntil, Studio }

object BookingRepeatForm {

    type Data = BookingRepeat

    private type TupleType =
        (BookingRepeatFrequency.BookingRepeatFrequencyVal, Option[Int], Option[LocalDate])

    private val repeatFrequency =
        CustomFields.enumeration[BookingRepeatFrequency.BookingRepeatFrequencyVal](
            BookingRepeatFrequency.values.
                toSeq.
                map(_.asInstanceOf[BookingRepeatFrequency.BookingRepeatFrequencyVal]).
                map { v => v -> v.value },
            "Invalid booking repetition frequency.")

    val form: Form[Data] = Form(
        tuple(
            "frequency"                     -> repeatFrequency,
            "count"                         -> optional(number),
            "until"                         -> optional(CustomFields.localDate)
        ).
            verifying("Booking repeat value required.", {
                case ((_, countOpt, untilOpt)) => countOpt.isDefined || untilOpt.isDefined
            }: (TupleType) => Boolean).
            transform(
                {
                    case (freq, Some(count), None) => BookingRepeatCount(freq, count)
                    case (freq, None, Some(until)) => BookingRepeatUntil(freq, until)
                    // Should never be reached because of hereabove verifying constraint:
                    case _ =>
                        throw new UnsupportedOperationException("Invalid booking repeat state.")
                }: (TupleType) => BookingRepeat,
                {
                    case BookingRepeatCount(freq, count) => (freq, Some(count), None)
                    case BookingRepeatUntil(freq, until) => (freq, None, Some(until))
                }: BookingRepeat => TupleType
            ).
            verifying("Repeat count should be greater or equal to 1.", {
                case BookingRepeatCount(_, count) => count >= 1
                case _ => true
            }: BookingRepeat => Boolean)
    )
}
