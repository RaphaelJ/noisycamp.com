/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2021  Raphael Javaux <raphael@noisycamp.com>
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

import misc.URL

class URLSpec extends PlaySpec {

    "URL.titleAsURL" must {
        "remove accents" in {
            URL.titleAsURL("voilà") should be ("voila")
        }
        "use lower case letters" in {
            URL.titleAsURL("Javaux") should be ("javaux")
        }
        "changes spaces to '-'" in {
            URL.titleAsURL("United Kingdom") should be ("united-kingdom")
        }
        "change special characters to '-'" in {
            URL.titleAsURL("24/12/2020") should be ("24-12-2020")
        }
        "limit the total number of characters" in {
            URL.titleAsURL(
                "Le Petit Prince est une oeuvre de langue française, la plus connue d'Antoine de " +
                "Saint-Exupéry. Publié en 1943 à New York simultanément à sa traduction anglaise"
                ) should be (
                "le-petit-prince-est-une-oeuvre-de-langue-francaise--la-plus-")
        }
    }
}
