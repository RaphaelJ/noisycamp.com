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

import scala.concurrent.{ ExecutionContext, Future }

import com.mohiva.play.silhouette.api.Silhouette
import play.api._
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import auth.DefaultEnv
import daos.{ StudioDAO, StudioPictureDAO }
import forms.account.StudioForm
import misc.{ Country, Currency, ExchangeRateService }

import models.{ BookingPolicy, CancellationPolicy, EveningPricingPolicy,
  Location, OpeningSchedule, OpeningTimes, PricingPolicy, Studio, User,
  WeekendPricingPolicy }
import java.time.{ Duration, Instant, LocalTime }

@Singleton
class StudiosController @Inject() (
  cc: ControllerComponents,
  implicit val config: Configuration,
  protected val dbConfigProvider: DatabaseConfigProvider,
  studioDao: StudioDAO,
  studioPictureDao: StudioPictureDAO,
  implicit val executionContext: ExecutionContext,
  silhouette: Silhouette[DefaultEnv])
  extends AbstractController(cc)
  with I18nSupport
  with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  /** Lists all studios from a single user. */
  def index = silhouette.SecuredAction { implicit request =>
    Ok("")
  }

  /** Shows a form to list a new studio. */
  def create = silhouette.SecuredAction { implicit request =>
    Ok(views.html.account.studioCreate(request.identity, StudioForm.form))
  }

  def createSubmit = silhouette.SecuredAction.async { implicit request =>
    StudioForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.account.studioCreate(
        request.identity, StudioForm.form.bindFromRequest))),
      data => {
        val studio = Studio(
          ownerId = request.identity.id,
          name = data.name,
          description = data.description,
          location = data.location,
          openingSchedule = data.openingSchedule,
          pricingPolicy = data.pricingPolicy,
          bookingPolicy = data.bookingPolicy)

        db.run({
          for {
            studio <- studioDao.insert(studio)
            _ <- studioPictureDao.setStudioPics(studio.id, data.pictures)
          } yield Ok(studio.toString)
        }.transactionally)
      })
  }
}
