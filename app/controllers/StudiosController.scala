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

import java.time.{ DayOfWeek, Instant }
import javax.inject._
import scala.concurrent.Future
import scala.util.Try

import play.api._
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc._
import play.filters.headers.SecurityHeadersFilter

import daos.CustomColumnTypes
import daos.StudioBookingDAO.toStudioBooking
import forms.studios.SearchForm
import misc.GIS
import misc.JsonWrites._
import models.{ BBox, Studio, StudioWithPictureAndEquipments, StudioBooking, User }

@Singleton
class StudiosController @Inject() (ccc: CustomControllerCompoments)
    extends CustomBaseController(ccc)
    with CustomColumnTypes {

    import profile.api._

    def index = UserAwareAction { implicit request =>
        Ok(views.html.studios.index(identity=request.identity))
    }

    def search = UserAwareAction.async { implicit request =>
        SearchForm.form.bindFromRequest.fold(
            form => Future.successful(BadRequest("Invalid search parameters.")),
            data => {
                // Uses the BBox parameter, or generates a BBox around the location's
                // center if not provided.
                val bboxOpt = data.bbox.
                    orElse {
                        val bboxRadius = BigDecimal(60 * 1000) // 60 Km
                        data.center.map(GIS.centeredBBox(bboxRadius, _))
                    }

                db.run({ for {
                    // Fetches studios matching query.
                    studios <- daos.studio.publishedStudios.
                        filter { studio =>
                            // Geographical filter
                            val isInBBox = bboxOpt match {
                                case Some(BBox(north, south, west, east)) => {
                                    studio.long >= west && studio.long <= east &&
                                    studio.lat >= south && studio.lat <= north
                                }
                                case None => true: Rep[Boolean]
                            }

                            // Is open on date
                            val isOpen = data.availableOn match {
                                case Some(date) => {
                                    date.getDayOfWeek match {
                                        case DayOfWeek.MONDAY => studio.mondayIsOpen
                                        case DayOfWeek.TUESDAY => studio.tuesdayIsOpen
                                        case DayOfWeek.WEDNESDAY => studio.wednesdayIsOpen
                                        case DayOfWeek.THURSDAY => studio.thursdayIsOpen
                                        case DayOfWeek.FRIDAY => studio.fridayIsOpen
                                        case DayOfWeek.SATURDAY => studio.saturdayIsOpen
                                        case DayOfWeek.SUNDAY => studio.sundayIsOpen
                                    }
                                }
                                case None => true: Rep[Boolean]
                            }

                            // FIXME: does not filter out studios that are not availaible on
                            // the selected day.

                            isInBBox && isOpen
                        }.
                        take(200). // Limits to 200 studios
                        result

                    // Fetches matching studios' pictures
                    picIds <- DBIO.sequence(
                        studios.map { studio =>
                            daos.studioPicture.
                                withStudioPictureIds(studio.id).
                                take(1).
                                result.
                            headOption
                        })

                    // Fetches matching studios' equipment
                    equipments <- DBIO.sequence(
                        studios.map { studio =>
                            daos.studioEquipment.
                                withStudioEquipment(studio.id).
                                result
                        })

                  } yield studios zip picIds zip equipments

                }.transactionally).map { studios =>
                    val results = studios.
                        map { case ((studio, picId), equips) =>
                            val localEquips = equips.map(_.localEquipment(studio))
                            StudioWithPictureAndEquipments(studio, picId, localEquips)
                        }

                    Ok(Json.obj("results" -> Json.toJson(results)))
                }
            }
        )
    }

    def show(id: String): Action[AnyContent] = {
        Try(id.takeWhile(_.isDigit).toLong).toOption match {
            case Some(idLong: Long) => show(idLong)
            case None => Action(BadRequest("Invalid studio ID."))
        }
    }

    def show(id: Studio#Id): Action[AnyContent] = UserAwareAction.async { implicit request =>
        val user: Option[User] = request.identity.map(_.user)

        val now = Instant.now

        db.run(getStudioData(id, now))
            .map {
                case Some((studio, equips, picIds, bookingEvents)) if studio.canAccess(user) => {
                    Ok(views.html.studios.show(
                        identity = request.identity, now,
                        studio, equips, picIds, bookingEvents))
                }
                case _ => NotFound("Studio not found.")
            }
    }

    def embedded(studioId: Studio#Id)= UserAwareAction.async { implicit request =>
        val now = Instant.now

        // Only allows the embedded widget if the owner's plan allows it or if it's called from the
        // demo page.
        def isAllowed(owner: User): Boolean = {
            val demoUrl = account.studios.routes.EmbeddedController.demo(studioId).absoluteURL
            owner.plan.websiteIntegration ||
            request.headers.get("Referer").map(_ == demoUrl).getOrElse(false)
        }

        db.run(
            getStudioData(studioId, now).flatMap {
                case Some((studio, equips, picIds, bookingEvents)) => {
                    daos.user.query.
                        filter(_.id === studio.ownerId).
                        result.head.
                        map { owner => Some((studio, owner, equips, picIds, bookingEvents)) }
                }
                case None => DBIO.successful(None)
        }).map {
            case Some((studio, owner, equips, picIds, bookingEvents)) if isAllowed(owner) => {
                Ok(views.html.studios.embedded(
                    now, studio, equips, picIds, bookingEvents))
            }
            case Some(_) => Forbidden("Website integration not allowed.")
            case None => NotFound("Studio not found.")
        }
    }

    private def getStudioData(id: Studio#Id, now: Instant = Instant.now) = {
        for {
            studioOpt <- daos.studio.query.
                filter(_.id === id).
                result.headOption

            equips <- daos.studioEquipment.
                withStudioEquipment(id).
                result

            picIds <- daos.studioPicture.withStudioPictureIds(id).result

            bookingEvents <- studioOpt match {
                case Some(studio) => {
                    val localDate = studio.currentDateTime(now).toLocalDate
                    daos.studioBooking.activeBookings.
                        filter(_._1.studioId === studio.id).
                        filter(_._1.beginsAt >= localDate.atStartOfDay).
                        sortBy(_._1.beginsAt).
                        result.
                        // Converts bookings into anonymous calendar events.
                        map { bookings =>
                            bookings.map { booking =>
                                booking.
                                    toEvent.
                                    withClasses(Seq("occupied")).
                                    withTitle(None).
                                    withHref(None) }
                        }
                }
                case None => DBIO.successful(Seq.empty)
            }
        } yield {
            studioOpt.
                map { studio =>
                    (studio, equips.map(_.localEquipment(studio)), picIds, bookingEvents) }
        }
    }
}
