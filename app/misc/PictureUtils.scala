package misc

import java.security.MessageDigest
import java.nio.file.{ Files, Path }

import com.sksamuel.scrimage._
import com.sksamuel.scrimage.nio.{ GifWriter, JpegWriter, PngWriter }
import org.joda.time.DateTime

import models.Picture

object PictureUtils {
  
  /** Creates a `Picture` object from the given file. Does not return anything
   * with failed to open the file. */
  def fromFile(path: Path): Option[Picture] = {
    val content = Files.readAllBytes(path)
    
    FormatDetector.
      detect(content).
      map { format =>
        val hash = MessageDigest.getInstance("SHA-256").digest(content)
        Picture(id=hash, uploadedAt=new DateTime(), format=format,
          content=content)
      }
  }
  
  /** Resizes the given picture to fit in the requested dimensions if required.
   */
  def resize(picture: Picture, maxWidth: Option[Int], maxHeight: Option[Int])
    : Picture = {

    require(maxWidth.isDefined || maxHeight.isDefined)

    val img = Image(picture.content)
    val resizedImg = img.
      max(maxWidth.getOrElse(img.width), maxHeight.getOrElse(img.height))

    val writer = picture.format match {
        case Format.GIF => GifWriter.Default
        case Format.JPEG => JpegWriter.Default
        case Format.PNG => PngWriter.NoCompression
      }
    
    picture.copy(content=resizedImg.bytes(writer))
  }
}
