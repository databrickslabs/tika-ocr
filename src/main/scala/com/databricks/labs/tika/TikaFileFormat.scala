package com.databricks.labs.tika

import com.google.common.io.{ByteStreams, Closeables}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileStatus, Path}
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkException
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.execution.datasources.{FileFormat, OutputWriterFactory, PartitionedFile}
import org.apache.spark.sql.sources.{DataSourceRegister, Filter}
import org.apache.spark.sql.types._
import org.apache.spark.util.SerializableConfiguration
import org.apache.tika.io.TikaInputStream


class TikaFileFormat extends FileFormat with DataSourceRegister {

  // We do not infer the schema (such as CSV, JSON, etc. Our schema is fixed
  override def inferSchema(
                            sparkSession: SparkSession,
                            options: Map[String, String],
                            files: Seq[FileStatus]): Option[StructType] = Some(TikaSerializer.schema)

  // This is an input format only, we do not create write capabilities
  override def prepareWrite(
                             sparkSession: SparkSession,
                             job: Job,
                             options: Map[String, String],
                             dataSchema: StructType): OutputWriterFactory = {
    throw new UnsupportedOperationException("Write is not supported for tika file data source")
  }

  // Files are read as binary and need to be read as a whole (i.e. not split against multiple executors)
  override def isSplitable(
                            sparkSession: SparkSession,
                            options: Map[String, String],
                            path: Path): Boolean = {
    false
  }

  // We will enable our format to be used by its short name spark.read.format("tika")
  // Assuming we defined our parser in src/main/resources/META-INF
  override def shortName(): String = "tika"

  // Core business logic. We access our input stream and extract content from binary
  override protected def buildReader(
                                      sparkSession: SparkSession,
                                      dataSchema: StructType,
                                      partitionSchema: StructType,
                                      requiredSchema: StructType,
                                      filters: Seq[Filter],
                                      options: Map[String, String],
                                      hadoopConf: Configuration): PartitionedFile => Iterator[InternalRow] = {

    val hadoopConf_B = sparkSession.sparkContext.broadcast(new SerializableConfiguration(hadoopConf))
    val maxLength = sparkSession.conf.get("spark.sql.sources.binaryFile.maxLength").toInt

    // write limit of -1 means unlimited.
    // If not set, default is 100000 characters, making process to fail for large documents
    val bufferSize = getOption(options, TIKA_MAX_BUFFER_OPTION, "-1")(_.toInt)
    // Tesseract times out after 120 seconds by default, but we may want to modify this.
    val timeout = getOption(options, TESSERACT_TIMEOUT_SECONDS_OPTION, "120")(_.toInt)
    // Some files are considered too large to parse, so we give the option to override this
    val byteArrayMaxOverride = getOption(options, POI_IOUTILS_BYTEARRAYMAXOVERRIDE, "300000000")(_.toInt)
    // Temp files are usually not parseable (i.e. */~$* files). Optionally skip them.
    val skipOfficeTempFiles = getOption(options, TIKA_SKIP_OFFICE_TEMP_FILES, "true")(_.toBoolean)
    // Optionally skip password protected documents
    val skipEncryptedFiles = getOption(options, TIKA_SKIP_ENCRYPTED_FILES, "true")(_.toBoolean)
    // Optionally output XML
    val enableXMLOutput = getOption(options, TIKA_XML_OUTPUT, "false")(_.toBoolean)

    file: PartitionedFile => {

      // Retrieve file information
      val path = file.toPath
      val fs = path.getFileSystem(hadoopConf_B.value.value)
      val status = fs.getFileStatus(path)
      if (status.getLen > maxLength) {
        throw new SparkException(
          s"The length of ${status.getPath} is ${status.getLen}, " +
            s"which exceeds the max length allowed: $maxLength.")
      }
      val fileName = status.getPath.toString
      val fileLength = status.getLen
      val fileTime = DateTimeUtils.millisToMicros(status.getModificationTime)

      // Open file as a stream
      val inputStream = fs.open(status.getPath)

      try {

        // Fully read file content as a ByteArray
        val fileContent = ByteStreams.toByteArray(inputStream)
        val tikaInputStream = TikaInputStream.get(fileContent)

        // Extract text from binary using Tika
        val tikaContent = TikaExtractor.extract(tikaInputStream, fileName, bufferSize, timeout, byteArrayMaxOverride, enableXMLOutput)

        // Write content to a row following schema specs
        // Note: the required schema provided by spark may come in different order than previously defined
        val tikaDocument = ProcessedDocument(tikaContent, fileContent, fileName, fileTime, fileLength)

        // Return a serializable row of TIKA extracted content
        Iterator.single(TikaSerializer.serialize(tikaDocument, requiredSchema))
      } catch {
        case e: org.apache.tika.exception.TikaException =>
          e.getCause match {
            // Skip office temp files optionally
            case _: org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException if skipOfficeTempFiles =>
              Iterator.empty
            case _: org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException if skipEncryptedFiles =>
              // Skip encrypted files if the skipEncryptedFiles flag is true
              Iterator.empty
            case _ => throw e // Rethrow the TikaException if it's not a skip
          }
      } finally {
        Closeables.close(inputStream, true)
      }
    }
  }
}
