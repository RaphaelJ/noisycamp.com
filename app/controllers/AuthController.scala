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

import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{
  Credentials, PasswordHasherRegistry }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.exceptions.{
  IdentityNotFoundException, InvalidPasswordException }
import com.mohiva.play.silhouette.impl.providers.{
  CommonSocialProfile, CommonSocialProfileBuilder, CredentialsProvider,
  OAuth2Provider, OAuth2Settings, SocialProvider, SocialProviderRegistry,
  StatefulAuthInfo }
import com.mohiva.play.silhouette.impl.providers.state.UserStateItem

import play.api._
import play.api.mvc._

import auth.{ DefaultEnv, UserService }
import forms.auth.{ SignInForm, SignUpForm }

/** Provides endpoint to login and register using a email + password set. */
@Singleton
class AuthController @Inject() (
  ccc: CustomControllerCompoments,

  val authInfoRepository: AuthInfoRepository,
  val credentialsProvider: CredentialsProvider,
  val passwordHasherRegistry: PasswordHasherRegistry,
  val socialProviderRegistry: SocialProviderRegistry,
  implicit val userService: UserService)
  extends CustomBaseController(ccc) {

  def signIn(redirectTo: Option[String] = None) = silhouette.UserAwareAction {
    implicit request =>

    request.identity match {
      case Some(_) => redirectToResponse(redirectTo)
      case None => Ok(views.html.auth.signIn(
        SignInForm.form, socialProviderRegistry, redirectTo))
    }
  }

  def signInSubmit(redirectTo: Option[String] = None) =
    silhouette.UserAwareAction.async { implicit request =>

    request.identity match {
      case Some(_) => Future.successful(redirectToResponse(redirectTo))
      case None => {
        SignInForm.form.bindFromRequest.fold(
          form => Future.successful(
            BadRequest(views.html.auth.signIn(
              form, socialProviderRegistry, redirectTo))),
          data => {
            val credentials = Credentials(data.email, data.password)
            credentialsProvider.authenticate(credentials).
              flatMap { loginInfo =>
                userService.retrieve(loginInfo).flatMap {
                  case Some(_) => {
                    for {
                      authenticator <-
                        silhouette.env.authenticatorService.create(loginInfo)
                      v <- silhouette.env.authenticatorService.init(authenticator)
                      resp <- silhouette.env.authenticatorService.embed(
                        v, redirectToResponse(redirectTo))
                    } yield resp
                  }
                  case None => Future.failed(
                    new IdentityNotFoundException(
                      "Can not authenticate user with a password."))
                }
              }.
              recover {
                case _: InvalidPasswordException => {
                  val form = SignInForm.form.bindFromRequest.
                    withError("password", "Invalid password.")
                  BadRequest(views.html.auth.signIn(
                    form, socialProviderRegistry, redirectTo))
                }
                case _: IdentityNotFoundException => {
                  val form = SignInForm.form.bindFromRequest.
                    withError("email", "This user does not exists.")
                  BadRequest(views.html.auth.signIn(
                    form, socialProviderRegistry, redirectTo))
                }
              }
          }
        )
      }
    }
  }

  def signUp(redirectTo: Option[String] = None) = silhouette.UserAwareAction {
    implicit request =>

    request.identity match {
      case Some(_) => redirectToResponse(redirectTo)
      case None => Ok(views.html.auth.signUp(
        SignUpForm.form, socialProviderRegistry, redirectTo))
    }
  }

  def signUpSubmit(redirectTo: Option[String] = None) =
    silhouette.UserAwareAction.async { implicit request =>

    request.identity match {
      case Some(_) => Future.successful(redirectToResponse(redirectTo))
      case None =>  {
        SignUpForm.form.bindFromRequest.fold(
          form => Future.successful(
            BadRequest(views.html.auth.signUp(
              form, socialProviderRegistry, redirectTo))),
          data => {
            val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)

            userService.retrieve(loginInfo).flatMap {
              case Some(_) => {
                // User already exists with this email, notifies the user.
                val form = SignUpForm.form.bindFromRequest.
                  withError(
                    "email", "An account already exists with this email address.")

                Future.successful(
                  BadRequest(views.html.auth.signUp(
                    form, socialProviderRegistry, redirectTo)))
              }

              case None => {
                for {
                  _ <- userService.save(CommonSocialProfile(
                    loginInfo = loginInfo,
                    firstName = Some(data.firstName),
                    lastName = Some(data.lastName),
                    email = Some(data.email)))
                  authInfo <- authInfoRepository.add(
                    loginInfo, passwordHasherRegistry.current.hash(data.password))
                } yield redirectToResponse(redirectTo).
                  flashing("top-message" -> "Account successfully created.")
              }
            }
          }
        )
      }
    }
  }

  def oauth2Authenticate(provider: String, redirectTo: Option[String] = None) =
    Action.async { implicit request: Request[AnyContent] =>

    socialProviderRegistry.get[OAuth2Provider](provider) match {
      case Some(p: OAuth2Provider with CommonSocialProfileBuilder) => {
        // Saves the `redirectTo` value in the OAuth2 state
        val userState = UserStateItem(
          redirectTo match {
            case Some(url) => Map("redirect-to" -> url)
            case None => Map()
          })

        val newP = p.withSettings { settings =>
            // Sets the OAuth2 redirect URL to the current view.
            settings.copy(
              redirectURL = Some(routes.AuthController.
                oauth2Authenticate(provider).
                absoluteURL(secure = true))
            )
          }.asInstanceOf[OAuth2Provider with CommonSocialProfileBuilder]

        newP.
          authenticate(userState).
          flatMap {
          case Left(providerRedirect) => Future.successful(providerRedirect)
          case Right(StatefulAuthInfo(authInfo, userState)) => for {
            // Authentication successful

            // Retrieve the profile from the provider and creates/update the
            // local user account.
            profile <- newP.retrieveProfile(authInfo)
            _ <- userService.save(profile)

            // Saves the authentication token in the DB, sets the cookie and
            // redirects the user.
            authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
            authService = silhouette.env.authenticatorService
            authenticator <- authService.create(profile.loginInfo)
            value <- authService.init(authenticator)
            result <- authService.embed(value,
              redirectToResponse(userState.state.get("redirect-to")))
          } yield result
        }
      }
      case _ => Future.failed(new ProviderException(
        s"Cannot authenticate with unexpected social provider $provider"))
    }
  }

  private def redirectToResponse(redirectTo: Option[String]): Result = {
    val call = redirectTo match {
      case Some(url) => Call("GET", url)
      case None => routes.IndexController.index
    }

    Redirect(call)
  }
}
