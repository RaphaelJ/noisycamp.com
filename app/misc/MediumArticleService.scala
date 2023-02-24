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

package misc

import java.time.{ Duration, Instant }
import javax.inject._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

import play.api.Configuration
import play.api.libs.json.{ JsSuccess, JsValue, Reads }
import play.api.libs.ws._

import models.{ Picture, PictureId }
import pictures.PictureLoader

case class MediumArticle(
    title:              String,
    url:                String,
    thumbnail:          Option[PictureId],
    publicationDate:    Instant)

/** Provides a service that retreives the latest Medium articles from the blog.
 *
 * Previous result is cached and only refreshed once every hour.
 */
@Singleton
class MediumArticleService @Inject() (
    val config: Configuration,
    pictureLoader: PictureLoader,
    val ws: WSClient,
    implicit val executionContext: TaskExecutionContext) {

    private val ARTICLES_EXPIRE_DELAY = 3600 // sec

    private var articles: Option[(Seq[MediumArticle], Instant)] = None

    def getArticles(): Future[Option[Seq[MediumArticle]]] = {
        val now = Instant.now

        articles match {
            case Some((articles, lastFetch))
                if Duration.between(lastFetch, now).getSeconds < ARTICLES_EXPIRE_DELAY => {
                Future.successful(Some(articles))
            }
            case _ => {
                // No up to date article list.
                fetchArticles().
                    map { newArticles =>
                        articles = Some((newArticles, now))
                        Some(newArticles)
                    }.
                    fallbackTo(Future.successful(None))
            }
        }
    }

    private def fetchArticles(): Future[Seq[MediumArticle]] = {
        val mediumUsername = config.get[String]("noisycamp.mediumUsername")
        val mediumRss = f"https://medium.com/feed/@${mediumUsername}"
        val rss2jsonApiKey = config.get[String]("rss2json.apiKey")
        val url = "https://api.rss2json.com/v1/api.json" +
                  f"?rss_url=${mediumRss}&api_key=${rss2jsonApiKey}"

        val instantReads = Reads({ value =>
            val utcStr = value.as[String].patch(10, "T", 1).patch(19, "Z", 0)
            JsSuccess(Instant.parse(utcStr))
        })

        for {
            items <- ws.url(url).
                withFollowRedirects(true).
                get().
                map { response =>
                    (response.json \ "items").
                    as[Seq[JsValue]]
                }

            thumbnails <- Future.traverse(items) { item =>
                val thumbnailUrl = (item \ "thumbnail").as[String]
                pictureLoader.fromUrl(thumbnailUrl)
            }

            _ <- Future.traverse(thumbnails) { thumbnailOpt =>
                thumbnailOpt.
                    map(pictureLoader.toDatabase(_)).
                    getOrElse(Future.successful(None))
            }
        } yield items.zip(thumbnails).map { case (item, thumbnail) =>
            MediumArticle(
                (item \ "title").as[String],
                (item \ "link").as[String],
                thumbnail.map(_.id),
                (item \ "pubDate").as[Instant](instantReads))
        }
    }
}
