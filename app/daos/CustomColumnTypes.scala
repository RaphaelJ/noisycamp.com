/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019 2020  Raphael Javaux <raphael@noisycamp.com>
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

import scala.reflect.ClassTag
import java.time.{ Duration, LocalDateTime, LocalTime, ZoneId }

import com.sksamuel.scrimage.Format
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import squants.market

import i18n.{ Country, Currency }
import misc.EquipmentCategory
import models.{ PictureId, Plan, StudioBookingStatus }

trait CustomColumnTypes { this: HasDatabaseConfigProvider[JdbcProfile] =>

    import profile.api._

    /** Maps a hashable value of type `T` (such as `Enumeration.Value`) to as
     * string. */
    def enumeration[T: ClassTag](mapper: Seq[(T, String)]) = {
        val mapperMap = mapper.toMap
        val reverseMapperMap = mapper.map { case (k, v) => (v, k) }.toMap

        MappedColumnType.base[T, String](mapperMap, reverseMapperMap)
    }

    /** Maps a country value to its ISO code. */
    implicit val countryValType =
        MappedColumnType.base[Country.Val, String](_.isoCode, Country.byCode(_))

    /** Maps a currency value to its ISO code. */
    implicit val currencyType =
        MappedColumnType.base[market.Currency, String](_.code, Currency.byCode(_))

    /** Stores a java.time.Duration as an amount of milliseconds. */
    implicit val durationType = MappedColumnType.base[Duration, Long](_.toMillis, Duration.ofMillis)

    implicit val equipmentCategoryType =
        MappedColumnType.base[EquipmentCategory.Val, String](_.code, EquipmentCategory.byCode(_))

    /** Alternative implementation of java.time.LocalDateTime that maps to a
     * string. */
    implicit val localDateTimeType =
        MappedColumnType.base[LocalDateTime, String](_.toString, LocalDateTime.parse(_))

    /** Alternative implementation of java.time.LocalTime that maps to a string
     *
     * See <https://github.com/tminglei/slick-pg/issues/381> */
    implicit val localTimeType =
        MappedColumnType.base[LocalTime, String](_.toString, LocalTime.parse)

    implicit val pictureIdType =
        MappedColumnType.base[PictureId, Array[Byte]](_.value, PictureId.apply _)

    implicit val planType = enumeration[Plan.Val](Seq(
        Plan.Free       -> "free",
        Plan.Premium    -> "premium"))

    /** Stores Scrimage image format as text */
    implicit val scrimageFormatType = enumeration[Format](Seq(
        Format.PNG  -> "png",
        Format.GIF  -> "gif",
        Format.JPEG -> "jpeg"))

    implicit val studioBookingStatusValueType = enumeration[StudioBookingStatus.Value](Seq(
        StudioBookingStatus.PaymentProcessing   -> "payment-processing",
        StudioBookingStatus.PaymentFailure      -> "payment-failure",
        StudioBookingStatus.PendingValidation   -> "pending-validation",
        StudioBookingStatus.Valid               -> "valid",
        StudioBookingStatus.Rejected            -> "rejected",
        StudioBookingStatus.CancelledByCustomer -> "cancelled-by-customer",
        StudioBookingStatus.CancelledByOwner    -> "cancelled-by-owner"))

    implicit val zoneIdType = MappedColumnType.base[ZoneId, String](_.getId, ZoneId.of)
}
