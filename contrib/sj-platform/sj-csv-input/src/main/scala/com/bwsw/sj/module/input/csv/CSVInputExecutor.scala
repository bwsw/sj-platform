/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.module.input.csv

import com.bwsw.common.{AvroSerializer, JsonSerializer}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.utils.Tokenizer
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import com.bwsw.sj.common.utils.stream_distributor.{ByHash, StreamDistributor}
import com.bwsw.sj.common.utils.{AvroRecordUtils, StreamLiterals}
import com.opencsv.CSVParserBuilder
import io.netty.buffer.ByteBuf
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecord

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Implementation of Input Streaming Executor for CSV Input Module
  *
  * @param manager instance of InputEnvironmentManager used for receiving module's options
  * @author Pavel Tomskikh
  */
class CSVInputExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Record](manager) {
  private val jsonSerializer = new JsonSerializer
  private val avroSerializer = new AvroSerializer
  private val csvInputOptions = jsonSerializer.deserialize[CSVInputOptions](manager.options)
  private val uniqueKey = {
    if (csvInputOptions.uniqueKey.nonEmpty)
      csvInputOptions.uniqueKey
    else
      csvInputOptions.fields
  }

  private val tokenizer = new Tokenizer(csvInputOptions.lineSeparator, csvInputOptions.encoding)

  private val fieldsNumber = csvInputOptions.fields.length
  private val schema = {
    var scheme = SchemaBuilder.record("csv").fields()
    csvInputOptions.fields.foreach { field =>
      scheme = scheme.name(field).`type`().stringType().noDefault()
    }
    scheme.endRecord()
  }

  private val fallbackFieldName = "data"
  private val fallbackSchema = SchemaBuilder.record("fallback").fields()
    .name(fallbackFieldName).`type`().stringType().noDefault().endRecord()


  private val partitionCount = getPartitionCount(manager.outputs.find(_.name == csvInputOptions.outputStream).get)

  private val distributor = {
    if (csvInputOptions.distribution.isEmpty)
      new StreamDistributor(partitionCount)
    else
      new StreamDistributor(partitionCount, ByHash, csvInputOptions.distribution)
  }

  private val fallbackPartitionCount = getPartitionCount(manager.outputs.find(_.name == csvInputOptions.fallbackStream).get)
  private val fallbackDistributor = new StreamDistributor(fallbackPartitionCount)

  private val csvParser = {
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
    Try(csvParser.parseLine(line)) match {
      case Success(values) =>
        if (values.length == fieldsNumber) {
          val record = new Record(schema)
          csvInputOptions.fields.zip(values).foreach { case (field, value) => record.put(field, value) }
          val key = AvroRecordUtils.concatFields(uniqueKey, record)

          Some(InputEnvelope(
            s"${csvInputOptions.outputStream},$key",
            Seq((csvInputOptions.outputStream, distributor.getNextPartition(Some(record)))),
            record,
            Some(true)))
        } else {
          buildFallbackEnvelope(line)
        }

      case Failure(_) => buildFallbackEnvelope(line)
    }
  }

  override def serialize(obj: AnyRef): Array[Byte] =
    avroSerializer.serialize(obj.asInstanceOf[GenericRecord])

  private def buildFallbackEnvelope(data: String): Option[InputEnvelope[Record]] = {
    val record = new Record(fallbackSchema)
    record.put(fallbackFieldName, data)
    Some(InputEnvelope(
      s"${csvInputOptions.fallbackStream},$data",
      Seq((csvInputOptions.fallbackStream, fallbackDistributor.getNextPartition())),
      record))
  }

  private def getPartitionCount(streamDomain: StreamDomain) = {
    streamDomain match {
      case s: TStreamStreamDomain => s.partitions
      case s: KafkaStreamDomain => s.partitions
      case _ => throw new IllegalArgumentException(s"stream type must be ${StreamLiterals.tstreamType} or " +
        s"${StreamLiterals.kafkaStreamType}")
    }
  }
}