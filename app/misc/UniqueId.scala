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

package misc

import java.util.Formatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import play.api.Configuration

object UniqueId {
    val ALGO = "HmacSHA256"
    val MAX_ID_LEN = 256 / 4

    /** Generates a random but deterministic code from an ID.
     *
     * @param salt some value used as salt. Use this to identify the type of id to be generated
     * (e.g. "reservation-code").
     */
    def generate(
        salt: String, id: String, len: Int = MAX_ID_LEN)(
        implicit config: Configuration): String = {

        require(len <= MAX_ID_LEN)

        val key = config.get[String]("play.crypto.secret").getBytes

        val mac = Mac.getInstance(ALGO)
        mac.init(new SecretKeySpec(key, ALGO))

        val codeBytes = mac.doFinal(f"${salt}-${id}".toString.getBytes).toIndexedSeq

        toHexString(codeBytes).take(len)
    }

    def toHexString(value: Seq[Byte]) = {
        val formatter = new Formatter()
        for (b <- value) {
            formatter.format("%02x", b.asInstanceOf[Object])
        }
        formatter.toString
    }
}
