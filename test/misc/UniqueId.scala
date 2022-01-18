package misc

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

package test.models

import org.scalatest.Matchers._
import org.scalatestplus.play._

import misc.UniqueId

class StudioBookingSpec extends PlaySpec {
    "UniqueId.toHexString" must {
        "returns an hexadecimal representation of a byte sequence" in {
            UniqueId.toHexString(Seq(179.toByte, 63.toByte, 58.toByte)) should be ("b33f3a")
        }
    }
}