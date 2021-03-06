/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2020  Raphael Javaux <raphael@noisycamp.com>
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

package auth

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import javax.inject._
import play.api.mvc.{ ControllerComponents, RequestHeader }
import play.api.mvc.Results._

class CustomSecuredErrorHandler @Inject() (
    cc: ControllerComponents)
    extends SecuredErrorHandler {

    /**
     * Called when a user is not authenticated.
     *
     * Redirects to the sign-up page.
     */
    override def onNotAuthenticated(implicit request: RequestHeader) = {
        redirectToSignUp
    }

    /**
     * Called when a user is authenticated but not authorized.
     *
     * Redirects to the sign-up page.
     */
    override def onNotAuthorized(implicit request: RequestHeader) = {
        redirectToSignUp
    }

    private def redirectToSignUp(implicit request: RequestHeader) = {
        Future.successful(Redirect(controllers.routes.AuthController.signUp(Some(request.uri))))
    }
}
