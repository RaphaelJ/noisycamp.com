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

package controllers

import scala.concurrent.{ ExecutionContext, Future }

import com.mohiva.play.silhouette.api.{ LoginInfo, Silhouette }
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers.{
  CommonSocialProfile, CredentialsProvider, SocialProviderRegistry }
import javax.inject._
import play.api._
import play.api.i18n.I18nSupport
import play.api.mvc._

import auth.{ DefaultEnv, UserService }
import daos.{ UserDAO, UserLoginInfoDAO, UserPasswordInfoDAO }
import forms.auth.{ SignInForm, SignUpForm }

@Singleton
class AuthController @Inject() (
  authInfoRepository: AuthInfoRepository,
  cc: ControllerComponents,
  passwordHasherRegistry: PasswordHasherRegistry,
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  implicit val userService: UserService
  )(implicit executionContext: ExecutionContext)
  extends AbstractController(cc)
  with I18nSupport {

  def signIn = silhouette.UnsecuredAction {
    implicit request: Request[AnyContent] =>

    Ok(views.html.auth.signIn(SignInForm.form, socialProviderRegistry))
  }

  def signUp = silhouette.UnsecuredAction {
    implicit request: Request[AnyContent] =>

    Ok(views.html.auth.signUp(SignUpForm.form, socialProviderRegistry))
  }

  def signUpSubmit = silhouette.UnsecuredAction.async {
    implicit request: Request[AnyContent] =>

    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(
        BadRequest(views.html.auth.signUp(form, socialProviderRegistry))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)

        userService.retrieve(loginInfo).flatMap {
          case Some(user) => {
            // User already exists with this email, notifies the user.
            val form = SignUpForm.form.bindFromRequest.
              withError(
                "email", "An account already exists with this email address.")

            Future.successful(
              BadRequest(views.html.auth.signUp(form, socialProviderRegistry)))
          }

          case None => {
            for {
              user <- userService.save(CommonSocialProfile(
                loginInfo = loginInfo,
                firstName = Some(data.firstName),
                lastName = Some(data.lastName),
                email = Some(data.email)))
              authInfo <- authInfoRepository.add(
                loginInfo, passwordHasherRegistry.current.hash(data.password))
            } yield Redirect(routes.IndexController.index()).
              flashing("top-message" -> "Account successfully created.")
          }
        }
      }
    )
  }
}
