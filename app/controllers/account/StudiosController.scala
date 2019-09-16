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

import javax.inject._

import scala.concurrent.ExecutionContext

import com.mohiva.play.silhouette.api.Silhouette
import play.api._
import play.api.i18n.I18nSupport
import play.api.mvc._

import auth.DefaultEnv
import forms.account.StudioForm
import misc.{Currency, ExchangeRateService}

@Singleton
class StudiosController @Inject() (
  cc: ControllerComponents,
  implicit val config: Configuration,
  exchangeRateService: ExchangeRateService,
  implicit val executionContext: ExecutionContext,
  silhouette: Silhouette[DefaultEnv])
  extends AbstractController(cc)
  with I18nSupport {

  /** Lists all studios from a single user. */
  def index = silhouette.SecuredAction { implicit request =>
    Ok("")
  }

  /** Shows a form to list a new studio. */
  def create = silhouette.SecuredAction { implicit request =>
    val rates = exchangeRateService.exchangeRates
    val date = rates.date
    val ctx = rates.context
    println(date + ": " + Currency.USD(15).in(Currency.ISK)(ctx).toFormattedString)
    Ok(views.html.account.studioCreate(request.identity, StudioForm.form))
  }

  def createSubmit = silhouette.SecuredAction { implicit request =>
    StudioForm.form.bindFromRequest.fold(
      form => BadRequest(views.html.account.studioCreate(
        request.identity, StudioForm.form.bindFromRequest)),
      data => {
        Ok("")
      })
  }
}
