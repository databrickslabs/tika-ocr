package com.databricks.labs

package object tika {

  val TIKA_MAX_BUFFER_OPTION = "tika.parser.buffer.size"
  val TESSERACT_TIMEOUT_SECONDS_OPTION = "tika.parser.ocr.timeout"

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
}
