package com.databricks.labs

package object tika {

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
