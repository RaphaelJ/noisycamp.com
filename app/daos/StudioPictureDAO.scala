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

package daos

import scala.concurrent.ExecutionContext
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import models.{ Picture, StudioPicture, Studio }

class StudioPictureDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  val studioDao: StudioDAO,
  val pictureDao: PictureDAO)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class StudioPictureTable(tag: Tag)
    extends Table[StudioPicture](tag, "studio_picture") {

    def id          = column[StudioPicture#Id]("id", O.PrimaryKey, O.AutoInc)
    def studioId    = column[Studio#Id]("studio_id")
    def pictureId   = column[Picture#Id]("picture_id")

    def * = (id, studioId, pictureId).mapTo[StudioPicture]

    def studio = foreignKey(
      "fk_studio_picture_studio_id", studioId, studioDao.query)(_.id)

    def picture = foreignKey(
      "fk_studio_picture_picture_id", pictureId, pictureDao.query)(_.id)
  }

  lazy val query = TableQuery[StudioPictureTable]

  lazy val insert = query returning
    query.map(_.id) into ((stuPic, id) => stuPic.copy(id = id))

  /** Sets or updates the pictures associated with a studio.
   *
   * Should run in a transaction. */
  def setStudioPics(studioId: Studio#Id, pics: Seq[Picture#Id]) = {
    // Deletes any picture associations and inserts the new ones.
    DBIO.seq(
      query.filter(_.studioId === studioId).delete,
      query ++= pics.map { picId =>
        StudioPicture(studioId = studioId, pictureId = picId) }
    )
  }

  def withStudioPictureIds(id: Studio#Id) = {
    query.
      filter(_.studioId === id).
      sortBy(_.id).
      map(_.pictureId)
  }

  def getStudioWithPictures(id: Studio#Id)
    : DBIO[(Option[Studio], Seq[Picture#Id])] = {

      for {
        studio <- studioDao.query.
          filter(_.id === id).
          result.headOption

        picIds <- withStudioPictureIds(id).result
      } yield (studio, picIds)
  }
}
