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
import play.twirl.api.Html

import com.sendgrid.{ SendGrid, Method, Request, Response }
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{ Content, Email }

/** Provides an helper to send emails through SendGrid. */
@Singleton
class EmailService @Inject() (
    val config: Configuration,
    implicit val executionContext: TaskExecutionContext) {

    val sendGridInstance: SendGrid = new SendGrid(config.get[String]("sendgrid.apiKey"))

    val fromEmail: Email = new Email(config.get[String]("noisycamp.fromEmail"), "NoisyCamp")
    val replyToEmail: Email = new Email(config.get[String]("noisycamp.replyToEmail"), "NoisyCamp")

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

        println(request.getBody)

        Future {
            blocking {
                val response = sendGridInstance.api(request)
                println(response.getStatusCode())
                println(response.getBody())

                response
            }
        }
    }
}
