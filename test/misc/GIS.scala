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

package test.misc

import org.scalatest.Matchers._
import org.scalatestplus.play._

import misc.GIS
import models.Coordinates

class GISSpec extends PlaySpec {

  "GIS.centeredBBox" must {
    "work in the North hemisphere" in {
      val bbox = GIS.centeredBBox(10000, Coordinates(5.70315, 50.70358))

      bbox.north.toDouble should be (50.7935 +- 0.0001)
      bbox.south.toDouble should be (50.6136 +- 0.0001)
      bbox.west.toDouble  should be (5.5611 +- 0.0001)
      bbox.east.toDouble  should be (5.8451 +- 0.0001)
    }

    "work in the South hemisphere" in {
      val bbox = GIS.centeredBBox(45000, Coordinates(-22.9084, -43.1843))

      bbox.north.toDouble should be (-42.7796 +- 0.0001)
      bbox.south.toDouble should be (-43.5889 +- 0.0001)
      bbox.west.toDouble  should be (-23.4634 +- 0.0001)
      bbox.east.toDouble  should be (-22.3533 +- 0.0001)
    }

    "work according to the poles" in {
      val bbox = GIS.centeredBBox(25000000, Coordinates(0, 0))

      bbox.north.toDouble should be (90)
      bbox.south.toDouble should be (-90)
      bbox.west.toDouble  should be (-180)
      bbox.east.toDouble  should be (180)
    }
  }
}
