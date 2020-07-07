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

import misc.EquipmentCategory
import models.Equipment

class EquipmentDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CustomColumnTypes {

  import profile.api._

  final class EquipmentTable(tag: Tag)
    extends Table[Equipment](tag, "equipment") {

    def id            = column[Equipment#Id]("id", O.PrimaryKey, O.AutoInc)

    def category      = column[Option[EquipmentCategory.Val]]("category")
    def details       = column[Option[String]]("details")
    def pricePerHour  = column[Option[BigDecimal]]("price_per_hour")

    def * = (id, category, details, pricePerHour).mapTo[Equipment]
  }

  lazy val query = TableQuery[EquipmentTable]

  lazy val insert = query returning
    query.map(_.id) into ((equip, id) => equip.copy(id=id))
}