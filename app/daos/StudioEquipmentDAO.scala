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

import models.{ Equipment, Studio, StudioEquipment }

class StudioEquipmentDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  val studioDao: StudioDAO,
  val equipmentDao: EquipmentDAO)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class StudioEquipmentTable(tag: Tag)
    extends Table[StudioEquipment](tag, "studio_equipment") {

    def id            =
      column[StudioEquipment#Id]("id", O.PrimaryKey, O.AutoInc)

    def studioId      = column[Studio#Id]("studio_id")
    def equipmentId   = column[Equipment#Id]("equipment_id")

    def * = (id, studioId, equipmentId).mapTo[StudioEquipment]

    def studio = foreignKey(
      "fk_studio_equipment_studio_id", studioId, studioDao.query)(_.id)

    def equipment = foreignKey(
      "fk_studio_equipment_equipment_id", equipmentId, equipmentDao.query)(_.id)
  }

  lazy val query = TableQuery[StudioEquipmentTable]

  lazy val insert = query returning
    query.map(_.id) into ((stuEquip, id) => stuEquip.copy(id = id))

  /** Sets the equipments of the studio to the specified equipments.
   *
   * Inserts any non-existing equipments.
   *
   * Should run in a transaction. */
  def setStudioEquipments(studioId: Studio#Id, equipments: Seq[Equipment])
    : DBIO[Seq[StudioEquipment]] = {

    val existingEquip = equipments.filter(_.id != 0L)

    for {
      // Inserts the non-existing equipments.
      newEquips <- equipmentDao.insert ++= equipments.filter(_.id == 0L)

      // Replaces the previous equipment associations by the new ones.
      _ <- query.filter(_.studioId === studioId).delete
      studioEquips <- insert ++= (existingEquip ++ newEquips).map { equip =>
        StudioEquipment(studioId = studioId, equipmentId = equip.id) }
    } yield studioEquips
  }
}
