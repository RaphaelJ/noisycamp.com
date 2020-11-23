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

import scala.concurrent.{ Future, blocking }

import play.api.Configuration
import play.api.mvc.RequestHeader
import play.twirl.api.Html

import com.sendgrid.{ SendGrid, Method, Request, Response }
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{ Content, Email }

import forms.account.PremiumForm
import models.{ Picture, Studio, StudioBooking, User }

/** Provides an helper to send emails through SendGrid. */
@Singleton
class EmailService @Inject() (
    val config: Configuration,
    implicit val executionContext: TaskExecutionContext) {

    val sendGridInstance: SendGrid = new SendGrid(config.get[String]("sendgrid.apiKey"))

    val fromEmail: Email = new Email(config.get[String]("noisycamp.fromEmail"), "NoisyCamp")
    val replyToEmail: Email = new Email(config.get[String]("noisycamp.replyToEmail"), "NoisyCamp")

    def send(subject: String, content: play.twirl.api.Content, dest: User): Future[Response] = {
        send(subject, content, dest.email, dest.fullName)
    }

    def send(subject: String, content: play.twirl.api.Content, destEmail: String,
        destName: Option[String] = None): Future[Response] = {

        val sendgridContent = new Content(content.contentType, content.body)

        val dest = new Email(destEmail, destName.getOrElse(null))

        val mail = new Mail(fromEmail, subject, dest, sendgridContent)
        mail.setReplyTo(replyToEmail)

        val request = new Request()

        request.setMethod(Method.POST)
        request.setEndpoint("mail/send")
        request.setBody(mail.build())

        Future { blocking { sendGridInstance.api(request) } }
    }

    def sendPremiumRequest(user: User, data: PremiumForm.Data, studios: Seq[Studio])(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {

        // Submits the the form data to noisycamp.emailReplyTo
        val subject = f"Premium upgrade request - ${user.displayName}"
        val content = views.html.emails.premiumRequest(user, data, studios)
        val destEmail = replyToEmail.getEmail

        send(subject, content, destEmail)
    }

    /** Sends an email to the studio owner notifying them of an (automatically) accepted booking
     * request. */
    def sendBookingReceived(booking: StudioBooking, customer: User, studio: Studio, owner: User)(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {

        val content = views.html.emails.booking.received(booking, customer, studio, owner)
        send("Your studio has been booked", content, owner)
    }
        
    /** Sends an email to the studio owner notifying them of a new booking request. */ 
    def sendBookingRequest(booking: StudioBooking, customer: User, studio: Studio, owner: User)(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {

        val content = views.html.emails.booking.request(booking, customer, studio, owner)
        send("[Action required] Your studio received a new booking request", content, owner)
    }
    
    /** Sends an email to the customer notifying them that their booking request has been correctly
     * received and is currently in review. */ 
    def sendBookingRequestInReview(
        booking: StudioBooking, customer: User, studio: Studio)(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {
            
        val content = views.html.emails.booking.requestInReview(booking, customer, studio)
        send("Your booking request is in review", content, customer)
    }

    def sendBookingAccepted(
        booking: StudioBooking, customer: User, studio: Studio, pictures: Seq[Picture#Id], 
        owner: User)(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {

        val content = views.html.emails.booking.accepted(booking, customer, studio, pictures, owner)
        send("Your booking has been accepted", content, customer)
    }

    def sendBookingRejected(
        booking: StudioBooking, customer: User, studio: Studio)(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {

        val content = views.html.emails.booking.rejected(booking, customer, studio)
        send("Your booking has been rejected", content, customer)
    }

    def sendBookingCancelledByCustomer(
        booking: StudioBooking, customer: User, studio: Studio, owner: User)(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {

        val content = views.html.emails.booking.cancelledByCustomer(
            booking, customer, studio, owner)
        send(f"A booking has been cancelled", content, owner)
    }
    
    def sendBookingCancelledByOwner(booking: StudioBooking, customer: User, studio: Studio)(
        implicit request: RequestHeader, config: Configuration): Future[Response] = {

        val content = views.html.emails.booking.cancelledByOwner(booking, customer, studio)
        send(f"Your booking has been cancelled", content, customer)
    }
}
