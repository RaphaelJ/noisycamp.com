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

package pictures

import java.security.MessageDigest
import java.nio.file.{ Files, Path }

import com.sksamuel.scrimage._

import models.{ Picture, PictureId }

object PictureUtils {
    /** Creates a `Picture` object from the given file. Does not return anything
     * with failed to open the file. */
    def fromFile(path: Path): Option[Picture] = {
        val content = Files.readAllBytes(path)

        FormatDetector.
            detect(content).
            map { format =>
                val hash = MessageDigest.getInstance("SHA-256").digest(content)
                Picture(id=PictureId(hash), format=format, content=content)
            }
    }
}
