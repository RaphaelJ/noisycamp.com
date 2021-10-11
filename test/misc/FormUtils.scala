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

package test.misc

import javax.inject._
import scala.collection.JavaConverters._

import org.scalatest.Matchers._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ DefaultMessagesApi, Lang, MessagesImpl }
import play.api.libs.json.{ Json, JsNull, JsString }

import misc.FormUtils

class FormUtilsSpec extends PlaySpec with GuiceOneAppPerSuite {

    implicit val messagesProvider = MessagesImpl(
        Lang.defaultLang, new DefaultMessagesApi(
            Map.empty.asJava: java.util.Map[String, java.util.Map[String,String]]))

    "FormUtils.dataAsJson" must {

        val form = Form(
            tuple(
                "parent" -> tuple(
                    "child-1" -> tuple(
                        "sub-child-1" -> nonEmptyText,
                        "sub-child-2" -> nonEmptyText),
                    "child-2" -> nonEmptyText),
                "field" -> seq(nonEmptyText),
            ))

        "create a hierarchical JSON object from flat form data" in {

            val formData = Map(
                "parent.child-1.sub-child-1" -> "sub-child-1-value",
                "parent.child-2" -> "child-2-value",
                "field[1]" -> "field-value-1",
                "field[2]" -> "field-value-2",
                )

            val json = Json.obj(
                "parent" -> Json.obj(
                    "child-1" -> Json.obj(
                        "sub-child-1" -> "sub-child-1-value",
                        "sub-child-2" -> JsNull),
                    "child-2" -> "child-2-value"),
                "field" -> Json.obj(
                    "1" -> "field-value-1",
                    "2" -> "field-value-2"))

            FormUtils.dataAsJson(form.bind(formData)) should be (json)
        }

        "create a hierarchical JSON object from errors data" in {

            val formWithErrors = form.
                withError("parent.child-1.sub-child-1", "Field required.").
                withError("parent.child-1", "Value required.").
                withError("field[1]", "Value too small.").
                withGlobalError("Value will be overrided.").
                withGlobalError("Message too large.")

            val json = Json.obj(
                "" -> "Message too large.",
                "parent" -> Json.obj(
                    "child-1" -> Json.obj(
                        "" -> "Value required.",
                        "sub-child-1" -> "Field required.",
                        "sub-child-2" -> JsNull),
                    "child-2" -> JsNull),
                "field" -> Json.obj(
                    "1" -> "Value too small."))

            FormUtils.errorsAsJson(formWithErrors) should be (json)
        }
    }
}
