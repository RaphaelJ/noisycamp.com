/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphael@noisycamp.com>
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

import javax.inject._
import javax.mail.internet.InternetAddress

import scala.concurrent.{ Future, blocking }

import play.api.Configuration
import play.api.libs.mailer.{ Email, MailerClient}
import play.api.mvc.RequestHeader
import play.twirl.api.Html

import forms.account.PremiumForm
import models.{ LocalEquipment, Picture, Studio, StudioCustomerBooking, User }
import models.StudioManualBooking

/** Provides an helper to send emails through SendGrid. */
@Singleton
class EmailService @Inject() (
    val config: Configuration,
    val mailerClient: MailerClient,
    implicit val executionContext: TaskExecutionContext) {

    val fromEmail: String = EmailService.destAsInternetString(
        config.get[String]("noisycamp.fromEmail"), Some("NoisyCamp"))
    val replyToEmail: String = EmailService.destAsInternetString(
        config.get[String]("noisycamp.replyToEmail"), Some("NoisyCamp"))

    def send(subject: String, content: play.twirl.api.Content, dest: User): Future[String] = {
        send(subject, content, dest.email, dest.fullName)
    }

    def send(subject: String, content: play.twirl.api.Content, destEmail: String,
        destName: Option[String] = None): Future[String] = {

        val dest = EmailService.destAsInternetString(destEmail, destName)

        val email = Email(
            subject,
            fromEmail,
            Seq(dest),
            replyTo = Seq(replyToEmail),
            bodyHtml = Some(content.body)
        )

        Future { blocking { mailerClient.send(email) } }
    }

    def sendPremiumRequest(user: User, data: PremiumForm.Data, studios: Seq[Studio])(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        // Submits the the form data to noisycamp.emailReplyTo
        val subject = f"Premium upgrade request - ${user.displayName}"
        val content = views.html.emails.premiumRequest(user, data, studios)
        val destEmail = replyToEmail

        send(subject, content, destEmail)
    }

    /** Sends an email to the studio owner notifying them of an (automatically) accepted booking
     * request. */
    def sendBookingReceived(
        booking: StudioCustomerBooking, customer: User, studio: Studio, owner: User,
        equips: Seq[LocalEquipment])(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        val content = views.html.emails.booking.received(booking, customer, studio, owner, equips)
        send("Your studio has been booked", content, owner)
    }

    /** Sends an email to the studio owner notifying them of a new booking request. */
    def sendBookingRequest(
        booking: StudioCustomerBooking, customer: User, studio: Studio, owner: User,
        equips: Seq[LocalEquipment])(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        val content = views.html.emails.booking.request(booking, customer, studio, owner, equips)
        send("[Action required] Your studio received a new booking request", content, owner)
    }

    /** Sends an email to the customer notifying them that their booking request has been correctly
     * received and is currently in review. */
    def sendBookingRequestInReview(
        booking: StudioCustomerBooking, customer: User, studio: Studio)(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        val content = views.html.emails.booking.requestInReview(booking, customer, studio)
        send("Your booking request is in review", content, customer)
    }

    def sendBookingAccepted(
        booking: StudioCustomerBooking, customer: User, studio: Studio, pictures: Seq[Picture#Id],
        owner: User, equips: Seq[LocalEquipment])(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        val content = views.html.emails.booking.accepted(
            booking, customer, studio, pictures, owner, equips)
        send("Your booking has been accepted", content, customer)
    }

    def sendBookingRejected(
        booking: StudioCustomerBooking, customer: User, studio: Studio)(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        val content = views.html.emails.booking.rejected(booking, customer, studio)
        send("Your booking has been rejected", content, customer)
    }

    def sendBookingCancelledByCustomer(
        booking: StudioCustomerBooking, customer: User, studio: Studio, owner: User,
        equips: Seq[LocalEquipment])(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        val content = views.html.emails.booking.cancelledByCustomer(
            booking, customer, studio, owner, equips)
        send(f"A booking has been cancelled", content, owner)
    }

    def sendCustomerBookingCancelledByOwner(
        booking: StudioCustomerBooking, customer: User, studio: Studio,
        equips: Seq[LocalEquipment])(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        val content = views.html.emails.booking.customerBookingCancelledByOwner(
            booking, customer, studio, equips)
        send(f"Your booking has been cancelled", content, customer)
    }

    def sendManualBookingCreatedByOwner(
        booking: StudioManualBooking, studio: Studio, owner: User, pictures: Seq[Picture#Id])(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        require(booking.customerEmail.isDefined)

        val content = views.html.emails.booking.createdByOwner(booking, studio, owner, pictures)
        send(f"Your booking has been created", content, booking.customerEmail.get)
    }

    def sendManualBookingCancelledByOwner(booking: StudioManualBooking, studio: Studio)(
        implicit request: RequestHeader, config: Configuration): Future[String] = {

        require(booking.customerEmail.isDefined)

        val content = views.html.emails.booking.manualBookingCancelledByOwner(booking, studio)
        send(f"Your booking has been cancelled", content, booking.customerEmail.get)
    }
}

object EmailService {
    def destAsInternetString(destEmail: String, destName: Option[String]): String = {
        destName.
            map(new InternetAddress(destEmail, _)).
            getOrElse(new InternetAddress(destEmail)).
            toString
    }
}
