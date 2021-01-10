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

package controllers

import javax.inject._

import play.api._
import play.api.mvc._

@Singleton
class SitemapController @Inject() (ccc: CustomControllerCompoments) 
    extends CustomBaseController(ccc) {

    import profile.api._

    private object ChangeFreq extends Enumeration {
        case class Val(val value: String) extends super.Val

        val Always = Val("always")
        val Hourly = Val("hourly")
        val Daily = Val("daily")
        val Weekly = Val("weekly")
        val Monthly = Val("monthly")
        val Yearly = Val("yearly")
        val Never = Val("never")
    }

    def index = Action.async { implicit request =>

        for {
            studioUrls <- db.
                run({ daos.studio.publishedStudios.map(_.id).result }.transactionally).
                map { studioIds => 
                    studioIds.map { id => 
                        url(routes.StudiosController.show(id), ChangeFreq.Weekly, 0.5)
                    }
                }
            
            staticUrls = Seq(
                url(routes.IndexController.index, ChangeFreq.Always, 1.0),

                url(routes.IndexController.about, ChangeFreq.Weekly, 0.5),
                url(routes.IndexController.terms, ChangeFreq.Weekly, 0.5),
                url(routes.IndexController.privacy, ChangeFreq.Weekly, 0.5),

                url(routes.StudiosController.index, ChangeFreq.Weekly, 0.75))
        } yield {
            val sitemap: scala.xml.Elem = 
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
                    {staticUrls}
                    {studioUrls}
                </urlset>
            
            Ok(sitemap)
        }
    }

    private def url(loc: Call, changeFreq: ChangeFreq.Val, priority: Double)(
        implicit req: RequestHeader):
        scala.xml.Elem = {

        val forceHttps = config.get[Boolean]("noisycamp.forceHttps")

        <url>
            <loc>{loc.absoluteURL(forceHttps)}</loc>    
            <changefreq>{changeFreq.value}</changefreq>
            <priority>{priority}</priority>
        </url>
    }
}
