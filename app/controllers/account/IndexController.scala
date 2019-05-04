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

package controllers.account

import com.mohiva.play.silhouette.api.Silhouette
import javax.inject._
import play.api._
import play.api.mvc._

import auth.DefaultEnv

@Singleton
class IndexController @Inject() (
  cc: ControllerComponents,
  implicit val config: Configuration,
  silhouette: Silhouette[DefaultEnv])
  extends AbstractController(cc) {

  def index = silhouette.SecuredAction { implicit request =>
    Ok(views.html.account.index(user=request.identity))
  }
}
