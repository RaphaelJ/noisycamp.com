/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2020  Raphael Javaux <raphaeljavaux@gmail.com>
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

package controllers.account.studios

import javax.inject._

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api._
import play.api.mvc._
import slick.jdbc.TransactionIsolation

import auth.DefaultEnv
import forms.account.StudioForm
import models.{ PaymentPolicy, Studio, User }
import _root_.controllers.{ CustomBaseController, CustomControllerCompoments }

@Singleton
class IndexController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc) {

    import profile.api._

    /** Lists all studios from a single host. */
    def index = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run({ for {
            studios <- daos.studio.query.
                filter(_.ownerId === user.id).
                result

            picIds <- DBIO.sequence(
                studios.map { studio =>
                    daos.studioPicture.
                        withStudioPictureIds(studio.id).
                        take(1).result.
                        headOption
                })
            } yield studios zip picIds
        }.transactionally).map { studios =>
            Ok(views.html.account.studios.index(request.identity, studios))
        }
    }

    /** Shows a form to list a new studio. */
    def create = SecuredAction.async { implicit request =>
        val user = request.identity.user

        ifUserCanCreateStudio(
            DBIO.successful(
                Ok(views.html.account.studios.create(
                    request.identity, StudioForm.form(user.plan.equipmentFee)))))
    }

    def createSubmit = SecuredAction.async { implicit request =>
        val user = request.identity.user

        StudioForm.form(user.plan.equipmentFee).bindFromRequest.fold(
            form => Future.successful(
                BadRequest(views.html.account.studios.create(request.identity, form))),
            data => {
                val (studio, equipments, pictures) = data.toStudio(Left(request.identity.user.id))

                ifUserCanCreateStudio {
                    for {
                        studio <- daos.studio.insert(studio)
                        _ <- daos.studioEquipment.setStudioEquipments(studio.id, equipments)
                        _ <- daos.studioPicture.setStudioPics(studio.id, pictures)
                    } yield 
                        Redirect(_root_.controllers.routes.StudiosController.show(studio.URLId)).
                            flashing("success" ->
                                ("Your studio page is ready. " +
                                "You can now review it before making it available to the public."))
                }
            }
        )
    }

    /** Executes the provided action if the user is allow to create one additional studio (based on
     * on their plan), or redirect to the plan upgrade page. */
    private def ifUserCanCreateStudio[B](
        f: => DBIOAction[Result, NoStream, Effect.All])(
        implicit request: SecuredRequest[DefaultEnv, B]) = {

        val user = request.identity.user

        val redirectTo = _root_.controllers.account.routes.PremiumController.upgrade

        db.run({
            daos.studio.query.
                filter(_.ownerId === user.id).
                length.
                result.
                flatMap { nStudios =>
                    user.plan.studioLimit match {
                        case Some(studioLimit) if (studioLimit <= nStudios) => {
                            val result = Redirect(redirectTo).
                                flashing("error" -> 
                                    "Upgrade to NoisyCamp Premium to host more studios.")
                            DBIO.successful(result)
                        }
                        case _ => f
                    }
                }
        }.withTransactionIsolation(TransactionIsolation.Serializable))
    }

    def websiteIntegration(id: Studio#Id) = SecuredAction { implicit request =>
        val user = request.identity.user

        if (user.plan.websiteIntegration) {
            Ok("Feature not yet implemented.")
        } else {
            Redirect(_root_.controllers.account.routes.PremiumController.upgrade).
                flashing("error" -> 
                    ("Upgrade to NoisyCamp Premium to integrate NoisyCamp into your website."))
        }
    }

    /** Shows the form with the studio settings. */
    def settings(id: Studio#Id) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run {
            for {
                studio <- daos.studio.query.
                    filter(_.id === id).
                    result.headOption

                equips <- daos.studioEquipment.withStudioEquipment(id).result
                picIds <- daos.studioPicture.withStudioPictureIds(id).result
            } yield (studio, equips, picIds)
        }.map {
            case (Some(studio), equips, picIds) if studio.ownerId == user.id => {
                val form = StudioForm.fromStudio(user.plan.equipmentFee, studio, equips, picIds)

                Ok(views.html.account.studios.settings(request.identity, studio, form))
            }
            case (Some(studio), _, _) => Forbidden("Only the studio owner can edit settings.")
            case (None, _, _) => NotFound("Studio not found.")
        }
    }

    /** Validates and saves the new studio's settings. */
    def settingsSubmit(id: Studio#Id) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run({
            val dbStudio = daos.studio.query.
                filter(_.id === id).
                result.headOption

            dbStudio.flatMap { (_ match {
                case Some(studio) if studio.ownerId == user.id => {
                    StudioForm.form(user.plan.equipmentFee).bindFromRequest.fold(
                        form => DBIO.successful(BadRequest(
                            views.html.account.studios.settings(request.identity, studio, form))),
                        data => {
                            val (newStudio, newEquips, newPics) =
                                data.toStudio(Right(studio))

                            val URLId = newStudio.URLId
                            val onSuccess = {
                                Redirect(_root_.controllers.routes.StudiosController.show(URLId)).
                                    flashing("success" -> "Settings have been successfully saved.")
                            }

                            DBIO.seq(
                                daos.studio.query.
                                    filter(_.id === id).
                                    update(newStudio),
                                daos.studioEquipment.setStudioEquipments(id, newEquips),
                                daos.studioPicture.setStudioPics(id, newPics)
                            ).map { _ => onSuccess }
                        })
                }
                case Some(studio) => DBIO.successful(
                    Forbidden("Only the studio owner can edit settings."))
                case None => DBIO.successful(NotFound("Studio not found."))
            }): (Option[Studio] => DBIOAction[Result, slick.dbio.NoStream, Effect.All]) }
        }.transactionally)
    }

    def publish(id: Studio#Id) = SecuredAction.async { implicit request =>
        val user = request.identity.user

        db.run({
            val dbStudio = daos.studio.query.
                filter(_.id === id).
                result.headOption

            dbStudio.flatMap {
                case Some(studio) if studio.isOwner(user) => {
                    val onSuccess =
                        Redirect(_root_.controllers.routes.StudiosController.show(studio.URLId)).
                            flashing("success" -> "The studio is now visible to the public.")

                    daos.studio.query.
                        filter(_.id === id).
                        map(_.published).
                        update(true).
                        map { _ => onSuccess }
                    }
                case Some(studio) => DBIO.successful(
                    Forbidden("Only the studio owner can publish."))
                case None => DBIO.successful(
                    NotFound("Studio not found."))
            }
        }.transactionally)
    }
}
