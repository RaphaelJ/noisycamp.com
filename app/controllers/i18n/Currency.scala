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

package controllers.i18n

import javax.inject._

import play.api._
import play.api.mvc._

import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }

@Singleton
class Currency @Inject() (ccc: CustomControllerCompoments)
  extends CustomBaseController(ccc) {

  /** Sets the currency session variable to the requested currency. */
  def set(currencyCode: String, redirectTo: Option[String]) =
    silhouette.UserAwareAction.async { implicit request =>

    getClientConfig.map { clientConfig =>

      val result = Redirect(redirectTo.
        getOrElse(_root_.controllers.routes.IndexController.index.url))

      _root_.i18n.Currency.byCode.get(currencyCode) match {
        case Some(currency) => {
          result.withSession(
            request.session + ("config.currency" -> currency.code))
        }
        case None => result
      }
    }
  }
}
