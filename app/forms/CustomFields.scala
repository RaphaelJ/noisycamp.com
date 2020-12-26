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

package forms

import java.time.{ Duration, LocalDate, LocalDateTime, LocalTime }

import play.api.data.{ FormError, Mapping }
import play.api.data.format.Formatter
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import squants.market

import i18n.{ Country, Currency }
import models.{ BBox, Coordinates, Picture, PictureId }
import misc.EquipmentCategory

object CustomFields {

    /** Parses a number field as a money amount. */
    val amount: Mapping[BigDecimal] = {
        bigDecimal.verifying(min(BigDecimal(0.0)))
    }

    /** Serializes a bounding box as a `north,south,west,east` string. */
    val bbox: Mapping[BBox] = of(new Formatter[BBox] {
        def bind(key: String, data: Map[String, String]) = {
            def parse(value: String) = {
                val coords = value.split(',').map(BigDecimal(_))
                BBox(coords(0), coords(1), coords(2), coords(3))
            }

            parsing(parse, "Invalid bounding box format", Nil)(key, data)
        }

        def unbind(key: String, value: BBox) = {
            val BBox(north, south, west, east) = value
            Map(key -> f"${north},${south},${west},${north},${south}")
        }
    })

    /** Serializes coordinates as a `longitude,latitude` string. */
    val coordinates: Mapping[Coordinates] = of(new Formatter[Coordinates] {
        def bind(key: String, data: Map[String, String]) = {
            def parse(value: String) = {
                val coords = value.split(',').map(BigDecimal(_))
                Coordinates(coords(0), coords(1))
            }

            parsing(parse, "Invalid coordinates", Nil)(key, data)
        }

        def unbind(key: String, value: Coordinates) = {
            Map(key -> f"${value.long},${value.lat}")
        }
    })

    /** Accepts any valid country value (ISO code) from the `Country`
     * enumeration. */
    val country: Mapping[Country.Val] = enumeration[Country.Val](
        Country.values.
            toSeq.
            map(_.asInstanceOf[Country.Val]).
            map { v => v -> v.isoCode }, "Invalid country code")

    /** Maps a currency code to a `Currency` value. */
    val currency: Mapping[market.Currency] = enumeration[market.Currency](
        Currency.currencies.toSeq.map { v => v -> v.code }, "Invalid currency code")

    /** Maps a hashable value of type `T` (such as `Enumeration.Value`) to as
     * string. */
    def enumeration[T](mapper: Seq[(T, String)],
        errorMsg: String = "Invalid enumeration value"): Mapping[T] = {

        val mapperMap = mapper.toMap
        val reverseMapperMap = mapper.map { case (k, v) => (v, k) }.toMap

        val enumFormat = new Formatter[T] {
            def bind(key: String, data: Map[String, String]) = {
                parsing(reverseMapperMap, errorMsg, Nil)(key, data)
            }

            def unbind(key: String, value: T) = Map(key -> mapperMap(value))
        }

        of(enumFormat)
    }

    val equipmentCategory: Mapping[EquipmentCategory.Val] =
        enumeration[EquipmentCategory.Val](
            EquipmentCategory.values.
            toSeq.
            map(_.asInstanceOf[EquipmentCategory.Val]).
            map { v => v -> v.code }, "Invalid equipment category")

    val longId: Mapping[Long] = longNumber(min = 0)

    /** Parses a `<input type="date">` as a LocalDate value. */
    val localDate: Mapping[LocalDate] = {
        val localDateFormat = new Formatter[LocalDate] {
            def bind(key: String, data: Map[String, String]) = {
                parsing(LocalDate.parse, "Invalid date format", Nil)(key, data)
            }

            def unbind(key: String, value: LocalDate) = Map(key -> value.toString)
        }

        of(localDateFormat)
    }

    /** Parses a `<input type="date">` as a LocalDate value. */
    val localDateTime: Mapping[LocalDateTime] = {
        val localDateTimeFormat = new Formatter[LocalDateTime] {
            def bind(key: String, data: Map[String, String]) = {
                parsing(LocalDateTime.parse, "Invalid date-time format", Nil)(key, data)
            }

            def unbind(key: String, value: LocalDateTime) = Map(key -> value.toString)
        }

        of(localDateTimeFormat)
    }

    /** Parses a `<input type="time">` as a LocalTime value. */
    val localTime: Mapping[LocalTime] = {
        val localTimeFormat = new Formatter[LocalTime] {
            def bind(key: String, data: Map[String, String]) = {
                parsing(LocalTime.parse, "Invalid time format", Nil)(key, data)
            }

            def unbind(key: String, value: LocalTime) = Map(key -> value.toString)
        }

        of(localTimeFormat)
    }

    /** Similar to `text`, but will bind empty string to a `None` value. */
    val optionalText: Mapping[Option[String]] = {
        text.transform(str => {
            str match {
                case "" => None
                case _ => Some(str)
            }
        },
        _ .getOrElse(""))
    }

    val pictureId: Mapping[Picture#Id] = {
        val pictureIdFormat: Formatter[Picture#Id] = new Formatter[Picture#Id] {
            def bind(key: String, data: Map[String, String]) = {
                parsing(PictureId.fromString, "Invalid picture ID", Nil)(key, data)
            }

            def unbind(key: String, value: Picture#Id) = {
                Map(key -> value.base64)
            }
        }

        of(pictureIdFormat)
    }

    /** Maps a number of seconds to a Joda's Duration instance. */
    val seconds: Mapping[Duration] = {
        longNumber(min = 0).transform(Duration.ofSeconds, _.getSeconds)
    }
}
