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

package controllers.account

import javax.inject._

import scala.concurrent.Future

import play.api.mvc._

import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }
import forms.account.PremiumForm

@Singleton
class PremiumController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc) {

    import profile.api._

    def upgrade = silhouette.SecuredAction { implicit request =>
        Ok(views.html.account.premium.upgrade(request.identity, PremiumForm.form))
    }

    def upgradeSubmit = silhouette.SecuredAction { implicit request =>

        PremiumForm.form.bindFromRequest.fold(
            form => {
                BadRequest(views.html.account.premium.upgrade(request.identity, form))
            },
            data => {
                Redirect(routes.PremiumController.upgrade).
                    flashing("success" ->
                        "Your request has been submitted, we will get back to you shortly.")
            }
        )
    }
}
