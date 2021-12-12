/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2021  Raphael Javaux <raphael@noisycamp.com>
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

import play.api._
import play.api.mvc._

@Singleton
class IndexController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc) {

    def index = UserAwareAction.async { implicit request =>
        for {
            articlesOpt <- ccc.mediumArticleService.getArticles
        } yield Ok(views.html.index(identity=request.identity, articlesOpt=articlesOpt))
    }

    def becomeAHost = UserAwareAction.async { implicit request =>
        for {
            currency <- clientCurrency
        } yield Ok(views.html.becomeAHost(identity=request.identity, currency=currency))
    }

    def about = UserAwareAction { implicit request =>
        Ok(views.html.about(identity=request.identity))
    }

    def terms = UserAwareAction { implicit request =>
        Ok(views.html.terms(identity=request.identity))
    }

    def privacy = UserAwareAction { implicit request =>
        Ok(views.html.privacy(identity=request.identity))
    }
}
