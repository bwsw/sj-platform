package com.bwsw.sj.module.input.csv

import java.io.ByteArrayOutputStream

import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.{InputStreamingExecutor, Interval}
import com.opencsv.CSVParserBuilder
import io.netty.buffer.ByteBuf
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.avro.io.EncoderFactory

import scala.io.Source

/**
  * Executor for work with csv.
  *
  * @author Pavel Tomskikh
  */
class CSVInputExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Array[Byte]](manager) {

  val outputStream: String = manager.options("output-stream").asInstanceOf[String]
  val fallbackStream: String = manager.options("fallback-stream").asInstanceOf[String]
  val lineSeparator: Byte = manager.options("line-separator").asInstanceOf[String].head.toByte
  val fieldSeparator: Option[Char] = manager.options.get("field-separator").asInstanceOf[Option[String]].map(_.head)
  val quoteSymbol: Option[Char] = manager.options.get("quote-symbol").asInstanceOf[Option[String]].map(_.head)
  val encoding: String = manager.options("encoding").asInstanceOf[String]
  val partition = 0

  val fields: Seq[String] = manager.options("fields").asInstanceOf[Seq[String]]
  val fieldsNumber = fields.length
  val schema = {
    var scheme = SchemaBuilder.record("csv").fields()
    fields.foreach { field =>
      scheme = scheme.name(field).`type`().stringType().noDefault()
    }
    scheme.endRecord()
  }

  val writer = new GenericDatumWriter[GenericRecord](schema)
  val writerOutput = new ByteArrayOutputStream()
  val encoder = EncoderFactory.get().binaryEncoder(writerOutput, null)

  val uniqueKey = manager.options.get("unique-key") match {
    case Some(uniqueFields: Seq[String]) => uniqueFields
    case _ => fields
  }

  val csvParser = {
    val csvParserBuilder = new CSVParserBuilder
    fieldSeparator.foreach(csvParserBuilder.withSeparator)
    quoteSymbol.foreach(csvParserBuilder.withQuoteChar)
    csvParserBuilder.build()
  }

  override def tokenize(buffer: ByteBuf): Option[Interval] = {
    val startIndex = buffer.readerIndex()
    val writerIndex = buffer.writerIndex()
    val endIndex = buffer.indexOf(startIndex, writerIndex, lineSeparator)

    if (endIndex != -1) Some(Interval(startIndex, endIndex))
    else None
  }

  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Array[Byte]]] = {
    val length = interval.finalValue - interval.initialValue
    val dataBuffer = buffer.slice(interval.initialValue, length)
    val data = new Array[Byte](length)
    dataBuffer.getBytes(0, data)
    buffer.readerIndex(interval.finalValue + 1)
    val line = Source.fromBytes(data, encoding).mkString
    val values = csvParser.parseLine(line)

    if (values.length == fieldsNumber) {

      val record = new Record(schema)
      fields.zip(values).foreach { case (field, value) => record.put(field, value) }
      writer.write(record, encoder)
      encoder.flush()
      val serialized = writerOutput.toByteArray
      writerOutput.reset()
      val key = uniqueKey.foldLeft("") { (acc, field) => acc + record.get(field) }

      Some(new InputEnvelope(
        key,
        Array((outputStream, partition)),
        true,
        serialized))
    } else {
      Some(new InputEnvelope(
        line,
        Array((fallbackStream, partition)),
        true,
        data))
    }
  }
}
