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
import scala.reflect.ClassTag

import com.mohiva.play.silhouette.api.Silhouette
import play.api.Configuration
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.i18n.I18nSupport
import play.api.mvc._
import slick.jdbc.JdbcProfile
import squants.market

import auth.{ DefaultEnv, UserService }
import daos.DAOs
import _root_.i18n.{ Country, Currency, ExchangeRatesService, GeoIpService,
  GeoIpLocation, TimeZoneService }
import misc.{ PaymentService, TaskExecutionContext }
import pictures.PictureCache

class CustomControllerCompoments @Inject() (
    val cc: ControllerComponents,
    val config: Configuration,

    val executionContext: ExecutionContext,
    val taskExecutionContext: TaskExecutionContext,

    // Database and DAOs
    val dbConfigProvider: DatabaseConfigProvider,
    val daos: DAOs,

    // Auth
    val silhouette: Silhouette[DefaultEnv],

    // Misc
    val exchangeRatesService: ExchangeRatesService,
    val geoIpService: GeoIpService,
    val paymentService: PaymentService,
    val timeZoneService: TimeZoneService)

/** Provides a base class for controllers and simplifies the injection process
 * for common used objects.
 */
abstract class CustomBaseController @Inject () (
    ccc: CustomControllerCompoments)
    extends AbstractController(ccc.cc)
    with HasDatabaseConfigProvider[JdbcProfile]
    with I18nSupport {

    implicit val config = ccc.config
    implicit val executionContext = ccc.executionContext
    /** Uses this execution context for blocking calls. */
    val taskExecutionContext = ccc.taskExecutionContext

    val dbConfigProvider = ccc.dbConfigProvider
    val daos = ccc.daos

    val silhouette = ccc.silhouette

    implicit val exchangeRatesService = ccc.exchangeRatesService
    implicit val geoIpService = ccc.geoIpService
    implicit val timeZoneService = ccc.timeZoneService
    implicit val paymentService = ccc.paymentService
}
