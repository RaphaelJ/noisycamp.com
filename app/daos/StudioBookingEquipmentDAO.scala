/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2020  Raphael Javaux <raphael@noisycamp.com>
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

import models.{ Equipment, StudioBooking, StudioBookingEquipment, StudioEquipment }

class StudioBookingEquipmentDAO @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider,
    val studioBookingDao: StudioBookingDAO,
    val equipmentDao: EquipmentDAO)
    (implicit executionContext: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

    import profile.api._

    final class StudioBookingEquipmentTable(tag: Tag)
        extends Table[StudioBookingEquipment](tag, "studio_booking_equipment") {

        def id            = column[StudioBookingEquipment#Id]("id", O.PrimaryKey, O.AutoInc)

        def bookingId     = column[StudioBooking#Id]("booking_id")
        def equipmentId   = column[Equipment#Id]("equipment_id")

        def * = (id, bookingId, equipmentId).mapTo[StudioBookingEquipment]

        def booking = foreignKey(
            "fk_studio_booking_equipment_booking_id", bookingId,
            studioBookingDao.bookingQuery)(_.id)

        def equipment = foreignKey(
            "fk_studio_booking_equipment_equipment_id", equipmentId, equipmentDao.query)(_.id)
    }

    lazy val query = TableQuery[StudioBookingEquipmentTable]

    lazy val insert = query returning
        query.map(_.id) into ((bookingEquip, id) => bookingEquip.copy(id = id))

    def withBookingEquipment(id: StudioBooking#Id) /* : Query[Seq[Equipment]] */ = {
        query.
            filter(_.bookingId === id).
            join(equipmentDao.query).on(_.equipmentId === _.id).
            map(_._2)
    }
}
