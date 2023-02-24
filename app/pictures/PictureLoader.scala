/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2022  Raphael Javaux <raphael@noisycamp.com>
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

package pictures

import java.security.MessageDigest
import java.nio.file.{ Files, NoSuchFileException, Path }
import javax.inject._

import scala.concurrent.Future

import com.sksamuel.scrimage.format.{ Format, FormatDetector }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.ws._
import slick.jdbc.JdbcProfile

import daos.PictureDAO
import misc.TaskExecutionContext
import models.{
    GifFormat, JpegFormat, Picture, PictureId, PictureSource, PictureFromDatabase, PictureFromFile,
    PictureFromStream, PictureFromUrl, PngFormat, WebPFormat }
import java.io.InputStream

@Singleton
class PictureLoader @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider,
    pictureDao: PictureDAO,
    val ws: WSClient
    )(implicit executionContext: TaskExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {

    import profile.api._

    /** Creates a `Picture` object from the given source. Does not return anything with failed to
     * load the file. */
    def fromSource(source: PictureSource): Future[Option[Picture]] = {
        source match {
            case PictureFromDatabase(id) => fromDatabase(id)
            case PictureFromFile(path) => fromFile(path)
            case PictureFromStream(_, stream) => fromStream(stream)
            case PictureFromUrl(url) => fromUrl(url)
        }
    }

    def fromBytes(content: Array[Byte]): Option[Picture] = {
        val format = FormatDetector.detect(content)

        if (format.isPresent) {
            val pictureFormat = format.get match {
                case Format.GIF => GifFormat
                case Format.JPEG => JpegFormat
                case Format.PNG => PngFormat
                case Format.WEBP => WebPFormat(isLossless = false)
            }

            val hash = MessageDigest.getInstance("SHA-256").digest(content)
            Some(Picture(id=PictureId(hash), format=pictureFormat, content=content))
        } else {
            None
        }
    }

    def fromDatabase(id: PictureId): Future[Option[Picture]] = {
        db.run {
            pictureDao.get(id).
                result.
                headOption
        }
    }

    def toDatabase(picture: Picture): Future[Picture] = pictureDao.insertIfNotExits(picture)

    def fromFile(path: Path): Future[Option[Picture]] = {
        Future {
            try {
                val content = Files.readAllBytes(path)
                fromBytes(content)
            } catch {
                case _: NoSuchFileException => None
            }
        }
    }

    def fromStream(stream: InputStream): Future[Option[Picture]] = {
        Future {
            val content = stream.readAllBytes()
            fromBytes(content)
        }
    }

    def fromUrl(url: String): Future[Option[Picture]] = {
        ws.url(url).
            withFollowRedirects(true).
            get().
            map { case response =>
                if (response.status >= 200 && response.status < 300) {
                    fromBytes(response.bodyAsBytes.toArray)
                } else {
                    None
                }
            }
    }
}
