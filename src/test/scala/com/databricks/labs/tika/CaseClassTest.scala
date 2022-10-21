package com.databricks.labs.tika

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.Charset

class CaseClassTest extends AnyFlatSpec with Matchers {

  "TikaContent class" should "be comparable" in {
    val c1 = TikaContent("hello, world", "text/plain", Map("foo" -> "bar"))
    val c2 = TikaContent("hello, world", "text/plain", Map("foo" -> "bar"))
    c1 shouldBe c2
  }

  it should "have correct values" in {
    val c1 = TikaContent("hello, world", "text/plain", Map("foo" -> "bar"))
    c1.content shouldBe "hello, world"
    c1.contentType shouldBe "text/plain"
    c1.metadata shouldBe Map("foo" -> "bar")
  }

  "ProcessedDocument class" should "be comparable" in {
    val c0 = TikaContent("hello, world", "text/plain", Map("foo" -> "bar"))
    val c1 = ProcessedDocument(c0, null, "foo.csv", 0L, 0L)
    val c2 = ProcessedDocument(c0, null, "foo.csv", 0L, 0L)
    c1 shouldBe c2
  }

  it should "have correct values" in {
    val c0 = TikaContent("hello, world", "text/plain", Map("foo" -> "bar"))
    val c1 = ProcessedDocument(c0, "TIKA".getBytes(Charset.defaultCharset()), "foo.csv", 0L, 0L)
    c1.tikaContent.content shouldBe "hello, world"
    c1.tikaContent.contentType shouldBe "text/plain"
    c1.tikaContent.metadata shouldBe Map("foo" -> "bar")
    new String(c1.fileContent, Charset.defaultCharset()) shouldBe "TIKA"
    c1.fileName shouldBe "foo.csv"
    c1.fileTime shouldBe 0L
    c1.fileLength shouldBe 0L
  }

}
