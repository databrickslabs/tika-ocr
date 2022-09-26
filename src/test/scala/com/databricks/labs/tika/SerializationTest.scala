package com.databricks.labs.tika

import org.apache.spark.sql.catalyst.expressions.UnsafeMapData
import org.apache.spark.sql.catalyst.expressions.codegen.UnsafeRowWriter
import org.apache.spark.sql.types.{MapType, StringType, StructField, StructType}
import org.apache.spark.unsafe.types.UTF8String
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.Charset

class SerializationTest extends AnyFlatSpec with Matchers {

  "Tika object" should "be serialized as row" in {
    val document = ProcessedDocument(
      TikaContent("hello, world", "text/plain", Map("foo" -> "bar")),
      "hello, world".getBytes(Charset.defaultCharset()),
      "foo.csv",
      0L,
      0L
    )
    val row = TikaSerializer.serialize(document, TikaSerializer.schema)
    row.numFields() shouldBe TikaSerializer.schema.fields.length
    TikaSerializer.schema.fields.map(_.dataType).zipWithIndex.map({ case (dataType, ordinal) =>
      val orow = row.get(ordinal, dataType)
      ordinal match {
        case 0 => orow.toString shouldBe "foo.csv"
        case 1 => orow.asInstanceOf[Long] shouldBe 0L
        case 2 => orow.asInstanceOf[Long] shouldBe 0L
        case 3 => new String(orow.asInstanceOf[Array[Byte]]) shouldBe "hello, world"
        case 4 => orow.toString shouldBe "text/plain"
        case 5 => orow.toString shouldBe "hello, world"
        case 6 =>
          val map = orow.asInstanceOf[UnsafeMapData]
          val numElements = map.keyArray().numElements()
          val reconstructed = (0 until  numElements).map(i => {
            (
              map.keyArray().get(i, StringType).toString,
              map.valueArray().get(i, StringType).toString
            )
          }).toMap
          reconstructed shouldBe Map("foo" -> "bar")
        case _ => fail("no more elements to process")
      }
    })
  }

  it should "fail with additional structType" in {
    val document = ProcessedDocument(
      TikaContent("hello, world", "text/plain", Map("foo" -> "bar")),
      "hello, world".getBytes(Charset.defaultCharset()),
      "foo.csv",
      0L,
      0L
    )
    assertThrows[RuntimeException] {
      val newSchema = StructType(TikaSerializer.schema.fields :+ StructField("hello", StringType, nullable = false))
      TikaSerializer.serialize(document, newSchema)
    }
  }

  "Created rows" should "be identical" in {
    val document = ProcessedDocument(
      TikaContent("hello, world", "text/plain", Map("foo" -> "bar")),
      "hello, world".getBytes(Charset.defaultCharset()),
      "foo.csv",
      0L,
      0L
    )
    val row1 = TikaSerializer.serialize(document, TikaSerializer.schema)
    val row2 = TikaSerializer.serialize(document, TikaSerializer.schema)
    row1 shouldBe row2
  }

  "A writer" should "write metadata as key value pair" in {
    val writer = new UnsafeRowWriter(1)
    writer.resetRowWriter()
    TikaSerializer.writeMetadata(writer, 0, Map("hello" -> "world"))
    val row = writer.getRow
    row.numFields() shouldBe 1
    val map = row.get(0, MapType(StringType, StringType))
    map match {
      case m: UnsafeMapData => m.keyArray().get(0, StringType).toString shouldBe "hello"
      case _ => fail("map not of right type")
    }
  }

  it should "not fail if empty" in {
    val writer = new UnsafeRowWriter(1)
    writer.resetRowWriter()
    TikaSerializer.writeMetadata(writer, 0, Map.empty[String, String])
    val row = writer.getRow
    row.numFields() shouldBe 1
  }

}
