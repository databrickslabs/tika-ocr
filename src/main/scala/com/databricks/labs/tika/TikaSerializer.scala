package com.databricks.labs.tika

import org.apache.spark.sql.catalyst.expressions.UnsafeRow
import org.apache.spark.sql.catalyst.expressions.codegen.{UnsafeArrayWriter, UnsafeRowWriter}
import org.apache.spark.sql.errors.QueryExecutionErrors
import org.apache.spark.sql.types.{BinaryType, LongType, MapType, StringType, StructField, StructType, TimestampType}
import org.apache.spark.unsafe.Platform
import org.apache.spark.unsafe.types.UTF8String

object TikaSerializer {

  private[tika] val COL_PATH = "path"
  private[tika] val COL_TIME = "modificationTime"
  private[tika] val COL_LENGTH = "length"
  private[tika] val COL_CONTENT = "content"
  private[tika] val COL_TYPE = "contentType"
  private[tika] val COL_TEXT = "contentText"
  private[tika] val COL_METADATA = "contentMetadata"

  private[tika] val schema: StructType = StructType(
    StructField(COL_PATH, StringType, nullable = false) ::
      StructField(COL_TIME, TimestampType, nullable = false) ::
      StructField(COL_LENGTH, LongType, nullable = false) ::
      StructField(COL_CONTENT, BinaryType, nullable = false) ::
      StructField(COL_TYPE, StringType, nullable = false) ::
      StructField(COL_TEXT, StringType, nullable = false) ::
      StructField(COL_METADATA, MapType(StringType, StringType), nullable = false) ::
      Nil
  )

  private[tika] def writeMetadata(writer: UnsafeRowWriter, offset: Int, metadata: Map[String, String]): Unit = {

    // MapType is not considered mutable, we need to convert to UnsafeRow record
    // This will be a structure for list of keys, list of values
    val previousCursor = writer.cursor()
    val dataType = MapType(StringType, StringType)
    val keyArrayWriter = new UnsafeArrayWriter(writer, dataType.defaultSize)
    val valArrayWriter = new UnsafeArrayWriter(writer, dataType.defaultSize)

    // preserve 8 bytes to write the key array numBytes later.
    valArrayWriter.grow(8)
    valArrayWriter.increaseCursor(8)

    // Write the keys and write the numBytes of key array into the first 8 bytes.
    keyArrayWriter.initialize(metadata.size)
    metadata.keys.zipWithIndex.foreach({ case (k, i) => keyArrayWriter.write(i, UTF8String.fromString(k))})

    // Shift cursor for array of values
    Platform.putLong(
      valArrayWriter.getBuffer,
      previousCursor,
      valArrayWriter.cursor - previousCursor - 8
    )

    // Write the values.
    valArrayWriter.initialize(metadata.size)
    metadata.values.zipWithIndex.foreach({ case (v, i) => valArrayWriter.write(i, UTF8String.fromString(v))})

    // Write our MapType - phew...
    writer.setOffsetAndSizeFromPreviousCursor(offset, previousCursor)

  }

  def serialize(processedDocument: ProcessedDocument, requiredSchema: StructType): UnsafeRow = {
    // Write content to a row following schema specs
    val writer = new UnsafeRowWriter(requiredSchema.length)
    writer.resetRowWriter()
    // Append each field to a row in the order dictated by our schema
    requiredSchema.zipWithIndex.foreach({ case (f, i) =>
      f.name match {
        case COL_PATH => writer.write(i, UTF8String.fromString(processedDocument.fileName))
        case COL_TIME => writer.write(i, processedDocument.fileTime)
        case COL_LENGTH => writer.write(i, processedDocument.fileLength)
        case COL_TEXT => writer.write(i, UTF8String.fromString(processedDocument.tikaContent.content))
        case COL_CONTENT => writer.write(i, processedDocument.fileContent)
        case COL_TYPE => writer.write(i, UTF8String.fromString(processedDocument.tikaContent.contentType))
        case COL_METADATA => writeMetadata(writer, i, processedDocument.tikaContent.metadata)
        case other => throw QueryExecutionErrors.unsupportedFieldNameError(other)
      }
    })

    writer.getRow
  }

}
