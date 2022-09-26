package com.databricks.labs.tika

import org.apache.tika.exception.TikaException
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.microsoft.OfficeParserConfig
import org.apache.tika.parser.ocr.TesseractOCRConfig
import org.apache.tika.parser.pdf.PDFParserConfig
import org.apache.tika.parser.{AutoDetectParser, ParseContext, Parser}
import org.apache.tika.sax.BodyContentHandler

import java.io.IOException
import scala.xml.SAXException

object TikaExtractor {

  @throws[IOException]
  @throws[SAXException]
  @throws[TikaException]
  def extract(stream: TikaInputStream, filename: String): TikaContent = {

    // Configure each parser if required
    val pdfConfig = new PDFParserConfig
    val officeConfig = new OfficeParserConfig
    val tesseractConfig = new TesseractOCRConfig
    val parseContext = new ParseContext

    val parser = new AutoDetectParser()
    val handler = new BodyContentHandler()
    val metadata = new Metadata()

    // To work, we need Tesseract library install natively
    // Input format will not fail, but won't be able to extract text from pictures
    parseContext.set(classOf[TesseractOCRConfig], tesseractConfig)
    parseContext.set(classOf[PDFParserConfig], pdfConfig)
    parseContext.set(classOf[OfficeParserConfig], officeConfig)
    parseContext.set(classOf[Parser], parser)

    try {

      // Tika will try at best effort to detect MIME-TYPE by reading some bytes in. With some formats such as .docx,
      // Tika is fooled thinking it is just another zip file. In our experience, it always works better when passing
      // a file than a stream as file name is also leveraged. So let's "fool the fool" by explicitly passing filename
      val contentType = retrieveContentType(parser, metadata, stream, filename)

      // Extract content using the appropriate parsing
      parser.parse(stream, handler, metadata, parseContext)
      val extractedTextContent = handler.toString

      // Return extracted content
      TikaContent(
        extractedTextContent,
        contentType,
        metadata.names().map(name => (name, metadata.get(name))).toMap
      )

    } finally {
      stream.close()
    }

  }

  def retrieveContentType(parser: AutoDetectParser,
                          metadata: Metadata,
                          stream: TikaInputStream,
                          filename: String): String = {
    metadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, filename)
    parser.getDetector.detect(stream, metadata).toString
  }

}
