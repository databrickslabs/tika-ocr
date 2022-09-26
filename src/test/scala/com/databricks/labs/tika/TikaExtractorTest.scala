package com.databricks.labs.tika

import org.apache.spark.SparkException
import org.apache.spark.sql.SparkSession
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.FileInputStream
import java.nio.file.Paths

class TikaExtractorTest extends AnyFlatSpec with Matchers {

  val spark: SparkSession = SparkSession.builder().appName("Tika").master("local[1]").getOrCreate()

  "A tika extractor" should "recognize content type" in {
    val parser = new AutoDetectParser()
    val metadata = new Metadata()
    val is = TikaInputStream.get(this.getClass.getResourceAsStream("/text/hello_tika.pdf"))
    val contentType = TikaExtractor.retrieveContentType(parser, metadata, is, "hello_tika.pdf")
    metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY) shouldBe "hello_tika.pdf"
    contentType shouldBe "application/pdf"
  }

  it should "extract text from PDF" in {
    val is = TikaInputStream.get(this.getClass.getResourceAsStream("/text/hello_tika.pdf"))
    val content = TikaExtractor.extract(is, "hello_tika.pdf").content
    content should include regex "[hH]ello"
    content should include regex "[tT]ika"
  }

  it should "extract content from a variety of files on disk" in {
    val path = Paths.get("src", "test", "resources", "text")
    val allFiles = path.toFile.listFiles()
    allFiles.length should be > 0
    allFiles.map(file => {
      val document = TikaExtractor.extract(TikaInputStream.get(new FileInputStream(file)), file.toString)
      val text = document.content
      text should include regex "[hH]ello"
      text should include regex "[tT]ika"
    })
  }

  it should "be able to extract content from a variety of images with Tesseract" in {
    val path = Paths.get("src", "test", "resources", "images")
    val allFiles = path.toFile.listFiles()
    allFiles.length should be > 0
    allFiles.map(file => {
      val document = TikaExtractor.extract(TikaInputStream.get(new FileInputStream(file)), file.toString)
      val text = document.content
      text should include regex "[hH]ello"
      text should include regex "[tT]ika"
    })
  }

  "A Spark input format" should "read files" in {
    val path1 = Paths.get("src", "test", "resources", "text").toAbsolutePath.toString
    val path2 = Paths.get("src", "test", "resources", "images").toAbsolutePath.toString
    val df = spark.read.format("com.databricks.labs.tika.TikaFileFormat").load(path1, path2)
    val corpus = df.select("contentText").cache
    corpus.count() shouldBe 15
    corpus.collect().map(_.getAs[String]("contentText")).foreach(text => {
      text should include regex "[hH]ello"
      text should include regex "[tT]ika"
    })
  }

  it should "not support write" in {
    assertThrows[UnsupportedOperationException] {
      new TikaFileFormat().prepareWrite(spark, null, Map.empty[String, String], TikaSerializer.schema)
    }
  }

  it should "support the use of short name" in {
    val path1 = Paths.get("src", "test", "resources", "text").toAbsolutePath.toString
    val path2 = Paths.get("src", "test", "resources", "images").toAbsolutePath.toString
    val df = spark.read.format("tika").load(path1, path2)
    val corpus = df.select("contentText").cache
    corpus.count() shouldBe 15
    corpus.collect().map(_.getAs[String]("contentText")).foreach(text => {
      text should include regex "[hH]ello"
      text should include regex "[tT]ika"
    })
  }

  it should "fail with max file limit" in {
    val path1 = Paths.get("src", "test", "resources", "text").toAbsolutePath.toString
    val path2 = Paths.get("src", "test", "resources", "images").toAbsolutePath.toString
    spark.conf.set("spark.sql.sources.binaryFile.maxLength", 10)
    assertThrows[SparkException] {
      val df = spark.read.format("tika").load(path1, path2)
      df.show()
    }
    spark.conf.unset("spark.sql.sources.binaryFile.maxLength")
  }

}
