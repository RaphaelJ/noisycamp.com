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

package forms.auth

import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import auth.UserService

object SignUpForm {

  def form(implicit userService: UserService) = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText,
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    firstName: String,
    lastName: String,
    email: String,
    password: String)
}
