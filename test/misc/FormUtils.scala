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

import org.scalatest.Matchers._
import org.scalatestplus.play._

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{ Json, JsString }

import misc.FormUtils

class FormUtilsSpect extends PlaySpec {

    "FormUtils.dataAsJson" must {
        "create a hierarchical JSON object from flat form data" in {
            val form = Form(
              tuple(
                "parent" -> tuple(
                    "child-1" -> tuple(
                        "sub-child-1" -> nonEmptyText,
                        "sub-child-2" -> nonEmptyText),
                    "child-2" -> nonEmptyText),
                "field" -> seq(nonEmptyText)))

            val formData = Map(
                "parent.child-1.sub-child-1" -> "sub-child-1-value",
                "parent.child-1.sub-child-2" -> "sub-child-2-value",
                "parent.child-2" -> "child-2-value",
                "field[1]" -> "field-value-1",
                "field[2]" -> "field-value-2")

            val json = Json.obj(
                "parent" -> Json.obj(
                    "child-1" -> Json.obj(
                        "sub-child-1" -> JsString("sub-child-1-value"),
                        "sub-child-2" -> JsString("sub-child-2-value")),
                    "child-2" -> JsString("child-2-value")),
                "field" -> Json.obj(
                    "1" -> "field-value-1",
                    "2" -> "field-value-2"))

            FormUtils.dataAsJson(form.bind(formData)) should be (json)
        }
    }
}
