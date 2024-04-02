package com.databricks.labs
import scala.util.{Failure, Success, Try}
import org.apache.spark.SparkException

package object tika {

  val TIKA_MAX_BUFFER_OPTION = "tika.parser.buffer.size"
  val TESSERACT_TIMEOUT_SECONDS_OPTION = "tika.parser.ocr.timeout"
  val TIKA_SKIP_OFFICE_TEMP_FILES = "tika.parser.skipOfficeTempFiles"
  val POI_IOUTILS_BYTEARRAYMAXOVERRIDE = "poi.ioutils.byteArrayMaxOverride"
  val TIKA_SKIP_ENCRYPTED_FILES = "tika.parser.skipEncryptedFiles"

  private[tika] case class TikaContent(
                                        content: String,
                                        contentType: String,
                                        metadata: Map[String, String]
                                      )

  private[tika] case class ProcessedDocument(
                                              tikaContent: TikaContent,
                                              fileContent: Array[Byte],
                                              fileName: String,
                                              fileTime: Long,
                                              fileLength: Long
                                            )

  def getOption[T](options: Map[String, String], optionKey: String, defaultValue: String)(parse: String => T = identity[String] _): T = {
    Try(parse(options.getOrElse(optionKey, defaultValue))) match {
      case Success(value) => value
      case Failure(exception) => throw new SparkException(s"Option [$optionKey] must be of the correct type. ${exception.getMessage}")
    }
  }
}
