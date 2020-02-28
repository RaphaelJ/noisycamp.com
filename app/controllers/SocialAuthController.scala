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

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers.{
  SocialProvider, SocialProviderRegistry, CommonSocialProfileBuilder }
import play.api._
import play.api.mvc._

import auth.UserService

@Singleton
class SocialAuthController @Inject() (
  ccc: CustomControllerCompoments,

  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  socialProviderRegistry: SocialProviderRegistry)
  extends CustomBaseController(ccc) {

  def authenticate(provider: String) = Action.async {
    implicit request: Request[AnyContent] =>

    socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(providerRedirect) => Future.successful(providerRedirect)
          case Right(authInfo) => for {
            // Authentication successful

            // Retrieve the profile from the provided and creates/update the
            // local user account.
            profile <- p.retrieveProfile(authInfo)
            _ <- userService.save(profile)

            // If there is a `?target=` URL parameter, redirect to that
            // location.
            target = request.queryString.get("target") match {
              case Some(url) => Call("GET", url.head)
              case None => routes.IndexController.index()
              }

            // Saves the authentication token in the DB, sets the cookie and
            // redirects the user.
            authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
            authService = silhouette.env.authenticatorService
            authenticator <- authService.create(profile.loginInfo)
            value <- authService.init(authenticator)
            result <- authService.embed(value, Redirect(target))
          } yield result
        }
      case _ => Future.failed(new ProviderException(
        s"Cannot authenticate with unexpected social provider $provider"))
    }
  }
}
