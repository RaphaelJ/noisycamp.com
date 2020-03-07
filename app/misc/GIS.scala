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
 *
 * Provides additional definitions over Squants market and currency manipulation
 * package.
 */

package misc

import scala.math

import models.{ BBox, Coordinates }

object GIS {
  /** Creates a bounding box around the provided center.
   *
   * @param radius is a value in meters. */
  def centeredBBox(radius: BigDecimal, center: Coordinates): BBox = {
    val earthRadius = 6371 * 1000 // 6371km

    // The length of a Earth parallel. This varies depending on the latitude.
    val parallelLength =
      2.0 * math.Pi * math.cos(center.lat.toDouble * math.Pi / 180.0) *
      earthRadius
    // The length of all meridians. This one is constant.
    val meridianLength = math.Pi * earthRadius

    // The length in meters of 1° around the two Earth axes.
    val parallelDegree = parallelLength / 360
    val meridianDegree = meridianLength / 180

    // Radius in degrees around provided center.
    val latRadius = radius / meridianDegree
    val longRadius = radius / parallelDegree

    BBox(
      (center.lat + latRadius) min 90,
      (center.lat - latRadius) max -90,
      (center.long - longRadius) max -180,
      (center.long + longRadius) min 180)
  }
}
