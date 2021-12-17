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
 *
 * Provides additional definitions over Squants market and currency manipulation
 * package.
 */

package misc

import scala.math

import models.BBox
import play.api.mvc.Call

case class HighlightLocationPicture(
    val url:        Call,
    val author:     String,
    val creditUrl:  String)

case class HighlightLocation(
    val id:             String,
    val name:           String,
    val bbox:           BBox,
    val picture:        HighlightLocationPicture
    ) {

    def url: Call = controllers.routes.IndexController.location(id)
}

object HighlightLocation {
    val locations = Seq(
        HighlightLocation(
            "australia", "Australia",
            BBox(-9.04366970633253, -54.835465496212, 112.821294400027, 159.208731278622),
            HighlightLocationPicture(
                controllers.routes.Assets.versioned("images/index/cities/australia.jpg"),
                "Johnny Bhalla",
                "https://unsplash.com/@johnnybhalla")),

        HighlightLocation(
            "belgium", "Belgium",
            BBox(51.5051159806493, 49.4969670113735, 2.51026658202508, 6.40815299714662),
            HighlightLocationPicture(
                controllers.routes.Assets.versioned("images/index/cities/belgium.jpg"),
                "Piron Guillaume",
                "https://unsplash.com/@gpiron")),

        HighlightLocation(
            "france", "France",
            BBox(51.148409399929, 41.3108229, -5.24227249992918, 9.66015649291518),
            HighlightLocationPicture(
                controllers.routes.Assets.versioned("images/index/cities/france.jpg"),
                "Jad Limcaco",
                "https://unsplash.com/@jadlimcaco")),

        HighlightLocation(
            "new-york", "New York",
            BBox(45.0239286969073, 40.4771391062446, -79.8578350999901, -71.7564918092633),
            HighlightLocationPicture(
                controllers.routes.Assets.versioned("images/index/cities/new-york.jpg"),
                "Tania Fernandez",
                "https://unsplash.com/@tania_fernandez")),

        HighlightLocation(
            "new-zealand", "New Zealand",
            BBox(-34.0465240000456, -52.6693956973145, 165.770163500618, 179.9),
            HighlightLocationPicture(
                controllers.routes.Assets.versioned("images/index/cities/new-zealand.jpg"),
                "Tobias Keller",
                "https://unsplash.com/@tokeller")),

        HighlightLocation(
            "united-kingdom", "The United Kingdom",
            BBox(60.9093517989553, 49.802416901086, -8.74974065661991, 1.86276379960989),
            HighlightLocationPicture(
                controllers.routes.Assets.versioned("images/index/cities/united-kingdom.jpg"),
                "Marcin Nowak",
                "https://unsplash.com/@marcin")),
        )

    val byId: Map[String, HighlightLocation] = locations.map(l => l.id -> l).toMap
}