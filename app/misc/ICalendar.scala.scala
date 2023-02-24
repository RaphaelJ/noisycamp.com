/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2022  Raphael Javaux <raphael@noisycamp.com>
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

import java.net.URI
import java.time.{ Instant, LocalDateTime, ZoneOffset }
import java.util.Date

import net.fortuna.ical4j.model.{ Calendar, DateTime, Recur, TimeZone, TimeZoneRegistryFactory }
import net.fortuna.ical4j.model.component.{ VEvent }
import net.fortuna.ical4j.model.property.{
    CalScale, Created, Description, DtEnd, DtStart, Location, Organizer, ProdId, Repeat, RRule,
    Status, Summary, Uid, Url, Version }
import play.api.Configuration
import play.api.mvc.RequestHeader

import models.{
    BookingRepeatCount, BookingRepeatUntil, Studio, StudioBooking, StudioBookingStatus,
    StudioCustomerBooking, StudioManualBooking, User }
import models.BookingRepeatFrequency
import com.sksamuel.scrimage.canvas.drawables.Rect
import models.BookingRepeatCount
import models.BookingRepeatUntil

object ICalendar {
    def fromStudioBookings(
        studio: Studio, bookings: Seq[(StudioBooking, Option[User])], now: Instant = Instant.now)(
        implicit req: RequestHeader, config: Configuration):
        Calendar = {

        val calendar = new Calendar()
        calendar.getProperties().add(new ProdId("-//NoisyCamp//Studio iCal Calendar//EN"))
        calendar.getProperties().add(Version.VERSION_2_0)
        calendar.getProperties().add(CalScale.GREGORIAN)

        for ((booking, customerOpt) <- bookings) {
            val event = fromStudioBooking(studio, booking, customerOpt, now)
            calendar.getComponents().add(event)
        }

        calendar
    }

    def fromStudioBooking(
        studio: Studio, booking: StudioBooking, customerOpt: Option[User],
        now: Instant = Instant.now)(
        implicit req: RequestHeader, config: Configuration):
        VEvent = {

        val timezone = timeZoneRegistry.getTimeZone(studio.timezone.toString)

        val createdAt = new DateTime(Date.from(studio.createdAt))

        def fromLocalDateTime(ldt: LocalDateTime): DateTime = {
            val calendar = java.util.Calendar.getInstance(timezone)
            calendar.set(
                ldt.getYear, ldt.getMonthValue - 1, ldt.getDayOfMonth,
                ldt.getHour, ldt.getMinute, ldt.getSecond)

            val date = calendar.getTime
            new DateTime(date, timezone)
        }

        val uid = f"${UniqueId.generate("booking-ical-id", booking.id.toString)}@noisycamp.com"

        val times = booking.times
        val start = fromLocalDateTime(times.beginsAt)
        val end = fromLocalDateTime(times.beginsAt plus times.duration)

        val descriptionBase = f"Reservation code: ${booking.reservationCode}"

        val (summary, description, organizerOpt) = booking match {
            case scb: StudioCustomerBooking => {
                val actionRequired =
                    scb.status == StudioBookingStatus.PendingValidation &&
                    !booking.isStarted(studio, now)

                val customerName = customerOpt.map(_.displayName).getOrElse("")
                val summary = if (actionRequired) f"⚠️ ${customerName}" else customerName

                val descriptionCustomer = customerOpt.
                    map { customer =>
                        "Customer: " +
                        customer.fullName.
                            map { fullName => f"${fullName} (${customer.email})" }.
                            getOrElse(customer.email)
                    }.
                    getOrElse("")

                val description =
                    if (actionRequired) {
                        "[Action required]\n" +
                        "Please open the booking request and either accept it or reject it. \n" +
                        "The booking request will be automatically rejected and the customer " +
                        "entirely refunded if not accepted before the session starts.\n\n" +
                        descriptionBase + "\n\n" +
                        descriptionCustomer
                    } else {
                        descriptionBase + "\n\n" +
                        descriptionCustomer
                    }


                val organizerOpt = customerOpt.
                    map { customer => new Organizer(f"mailto:${customer.email}") }

                (summary, description, organizerOpt)
            }
            case smb: StudioManualBooking => {
                val description = smb.customerEmail.
                    map { email =>
                        descriptionBase + "\n\n" +
                        "Customer: " + email
                    }.
                    getOrElse(descriptionBase)

                (smb.title, description, None)
            }
        }

        val url = controllers.account.studios.routes.BookingsController.
            show(studio.id, booking.id).
            absoluteURL(config.get[Boolean]("noisycamp.forceHttps"))

        val status = booking.status match {
            case StudioBookingStatus.PaymentProcessing => Status.VEVENT_TENTATIVE
            case StudioBookingStatus.PaymentFailure => Status.VEVENT_CANCELLED
            case StudioBookingStatus.PendingValidation => Status.VEVENT_TENTATIVE
            case StudioBookingStatus.Valid => Status.VEVENT_CONFIRMED
            case StudioBookingStatus.Rejected => Status.VEVENT_CANCELLED
            case StudioBookingStatus.CancelledByCustomer => Status.VEVENT_CANCELLED
            case StudioBookingStatus.CancelledByOwner => Status.VEVENT_CANCELLED
            case _ => throw new Exception("Invalid booking status.")
        }

        val event = new VEvent()
        val properties = event.getProperties()

        properties.add(new Uid(uid))

        properties.add(new Created(createdAt))
        properties.add(new DtStart(start))
        properties.add(new DtEnd(end))

        for (repeat <- times.repeat) {
            val freq = repeat.frequency match {
                case BookingRepeatFrequency.Daily => Recur.Frequency.DAILY
                case BookingRepeatFrequency.Weekly => Recur.Frequency.WEEKLY
                case BookingRepeatFrequency.Monthly => Recur.Frequency.MONTHLY
                case _ => Recur.Frequency.YEARLY
            }

            val recur = new Recur(freq, repeat.count(times.beginsAt.toLocalDate))

            properties.add(new RRule(recur))
        }

        properties.add(new Summary(summary))
        properties.add(new Location(studio.name))
        properties.add(new Url(new URI(url)))
        properties.add(status)

        properties.add(new Description(description))

        for (organizer <- organizerOpt) {
            properties.add(organizer)
        }

        event
    }

    private val timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry()
}

