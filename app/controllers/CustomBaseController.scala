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

import com.mohiva.play.silhouette.api.{ Env, Silhouette }
import com.mohiva.play.silhouette.api.actions.{
    SecuredActionBuilder, SecuredRequest, UnsecuredActionBuilder, UserAwareActionBuilder,
    UserAwareRequest }
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
import misc.{ EmailService, MediumArticleService, PaymentService, TaskExecutionContext }
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
    val emailService: EmailService,
    val exchangeRatesService: ExchangeRatesService,
    val geoIpService: GeoIpService,
    val mediumArticleService: MediumArticleService,
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

    /** Composes with the provided `composeWith` action builder to enforce the NoisyCamp SSL policy
     * when the user uses HTTP instead of HTTPS. */
    case class EnforceHttpsActionBuilder[P, R[_], A <: ActionBuilder[R, P]](
        composeWith: A,
        executionContext: ExecutionContext
        ) extends ActionBuilder[R, P] {

        override def invokeBlock[B](request: Request[B], block: R[B] => Future[Result]) = {
            val isSecure = request.secure || request.headers.get(X_FORWARDED_PROTO) == Some("https")

            if (isSecure || !config.get[Boolean]("noisycamp.forceHttps")) {
                composeWith.invokeBlock(request, block)
            } else {
                var queryString =
                    if (request.queryString.isEmpty) { "" }
                    else { f"?${request.rawQueryString}" }

                Future.successful(PermanentRedirect(
                    f"https://${request.host}${request.path}${queryString}"))
            }
        }

        override def parser = composeWith.parser
    }

    /** Requires the user to be signed in for the action to execute. */
    def SecuredAction = EnforceHttpsActionBuilder[
        AnyContent, ({ type R[B] = SecuredRequest[DefaultEnv, B] })#R,
        SecuredActionBuilder[DefaultEnv, AnyContent]](
        silhouette.SecuredAction, silhouette.SecuredAction.requestHandler.executionContext)

    /** Requires the user to not be signed in for the action to execute. */
    def UnsecuredAction = EnforceHttpsActionBuilder[
        AnyContent, Request, UnsecuredActionBuilder[DefaultEnv, AnyContent]](
        silhouette.UnsecuredAction, silhouette.UnsecuredAction.requestHandler.executionContext)

    /** Provides user information to the action. */
    def UserAwareAction = EnforceHttpsActionBuilder[
        AnyContent, ({ type R[B] = UserAwareRequest[DefaultEnv, B] })#R,
        UserAwareActionBuilder[DefaultEnv, AnyContent]](
        silhouette.UserAwareAction, silhouette.UserAwareAction.requestHandler.executionContext)

    val emailService = ccc.emailService
    implicit val exchangeRatesService = ccc.exchangeRatesService
    implicit val geoIpService = ccc.geoIpService
    implicit val timeZoneService = ccc.timeZoneService
    implicit val paymentService = ccc.paymentService

    def clientCountry[R <: RequestHeader](implicit request: R): Future[Option[Country.Val]] = {
        geoIpService.
            get(request.remoteAddress).
            map(_.map(_.country))
    }

    def clientCurrency[R <: RequestHeader](implicit request: R): Future[market.Currency] = {
        clientCountry.
            map(_.map(_.currency).
            getOrElse(Currency.default))
    }
}
