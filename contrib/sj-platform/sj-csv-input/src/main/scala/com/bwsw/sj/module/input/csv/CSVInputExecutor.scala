package com.bwsw.sj.module.input.csv

import java.io.IOException

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.{KafkaSjStream, SjStream, TStreamSjStream}
import com.bwsw.sj.common.utils.stream_distributor.{ByHash, SjStreamDistributor}
import com.bwsw.sj.common.utils.{AvroUtils, StreamLiterals}
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.utils.SeparateTokenizer
import com.bwsw.sj.engine.core.input.{InputStreamingExecutor, Interval}
import com.opencsv.CSVParserBuilder
import io.netty.buffer.ByteBuf
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record

import scala.io.Source

/**
  * Executor for work with csv.
  *
  * @author Pavel Tomskikh
  */
class CSVInputExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Record](manager) {
  private val serializer = new JsonSerializer
  private val csvInputOptions = serializer.deserialize[CSVInputOptions](manager.options)
  if (csvInputOptions.uniqueKey.isEmpty) csvInputOptions.uniqueKey = csvInputOptions.fields

  val tokenizer = new SeparateTokenizer(csvInputOptions.lineSeparator, csvInputOptions.encoding)

  val fieldsNumber = csvInputOptions.fields.length
  val schema = {
    var scheme = SchemaBuilder.record("csv").fields()
    csvInputOptions.fields.foreach { field =>
      scheme = scheme.name(field).`type`().stringType().noDefault()
    }
    scheme.endRecord()
  }

  val fallbackFieldName = "data"
  val fallbackSchema = SchemaBuilder.record("fallback").fields()
    .name(fallbackFieldName).`type`().stringType().noDefault().endRecord()


  val partitionCount = getPartitionCount(manager.outputs.find(_.name == csvInputOptions.outputStream).get)

  val distributor = {
    if (csvInputOptions.distribution.isEmpty) new SjStreamDistributor(partitionCount)
    else new SjStreamDistributor(partitionCount, ByHash, csvInputOptions.distribution)
  }

  val fallbackPartitionCount = getPartitionCount(manager.outputs.find(_.name == csvInputOptions.fallbackStream).get)
  val fallbackDistributor = new SjStreamDistributor(fallbackPartitionCount)

  val csvParser = {
    val csvParserBuilder = new CSVParserBuilder
    csvInputOptions.fieldSeparator.foreach(x => if (x.nonEmpty) csvParserBuilder.withSeparator(x.head))
    csvInputOptions.quoteSymbol.foreach(x => if (x.nonEmpty) csvParserBuilder.withQuoteChar(x.head))
    csvParserBuilder.build()
  }

  override def tokenize(buffer: ByteBuf): Option[Interval] = tokenizer.tokenize(buffer)

  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Record]] = {
    val length = interval.finalValue - interval.initialValue
    val dataBuffer = buffer.slice(interval.initialValue, length)
    val data = new Array[Byte](length)
    dataBuffer.getBytes(0, data)
    buffer.readerIndex(interval.finalValue + 1)
    val line = Source.fromBytes(data, csvInputOptions.encoding).mkString
    try {
      val values = csvParser.parseLine(line)

      if (values.length == fieldsNumber) {
        val record = new Record(schema)
        csvInputOptions.fields.zip(values).foreach { case (field, value) => record.put(field, value) }
        val key = AvroUtils.concatFields(csvInputOptions.uniqueKey, record)

        Some(new InputEnvelope(
          s"${csvInputOptions.outputStream}$key",
          Array((csvInputOptions.outputStream, distributor.getNextPartition(record))),
          true,
          record))
      } else {
        buildFallbackEnvelope(line)
      }
    } catch {
      case _: IOException => buildFallbackEnvelope(line)
    }
  }

  private def buildFallbackEnvelope(data: String): Option[InputEnvelope[Record]] = {
    val record = new Record(fallbackSchema)
    record.put(fallbackFieldName, data)
    Some(new InputEnvelope(
      s"${csvInputOptions.fallbackStream},$data",
      Array((csvInputOptions.fallbackStream, fallbackDistributor.getNextPartition())),
      false,
      record))
  }

  private def getPartitionCount(sjStream: SjStream) = {
    sjStream match {
      case s: TStreamSjStream => s.partitions
      case s: KafkaSjStream => s.partitions
      case _ => throw new IllegalArgumentException(s"stream type must be ${StreamLiterals.tstreamType} or " +
        s"${StreamLiterals.kafkaStreamType}")
    }
  }
}