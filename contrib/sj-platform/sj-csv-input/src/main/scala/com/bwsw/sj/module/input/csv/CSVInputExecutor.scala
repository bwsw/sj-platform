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

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.utils.stream_distributor.{ByHash, StreamDistributor}
import com.bwsw.sj.common.utils.{AvroRecordUtils, StreamLiterals}
import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.utils.Tokenizer
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import com.opencsv.CSVParserBuilder
import io.netty.buffer.ByteBuf
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Executor for work with csv.
  *
  * @author Pavel Tomskikh
  */
class CSVInputExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Record](manager) {
  private val serializer = new JsonSerializer
  private val csvInputOptions = serializer.deserialize[CSVInputOptions](manager.options)
  if (csvInputOptions.uniqueKey.isEmpty) csvInputOptions.uniqueKey = csvInputOptions.fields

  val tokenizer = new Tokenizer(csvInputOptions.lineSeparator, csvInputOptions.encoding)

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
    if (csvInputOptions.distribution.isEmpty) new StreamDistributor(partitionCount)
    else new StreamDistributor(partitionCount, ByHash, csvInputOptions.distribution)
  }

  val fallbackPartitionCount = getPartitionCount(manager.outputs.find(_.name == csvInputOptions.fallbackStream).get)
  val fallbackDistributor = new StreamDistributor(fallbackPartitionCount)

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
    Try(csvParser.parseLine(line)) match {
      case Success(values) =>
        if (values.length == fieldsNumber) {
          val record = new Record(schema)
          csvInputOptions.fields.zip(values).foreach { case (field, value) => record.put(field, value) }
          val key = AvroRecordUtils.concatFields(csvInputOptions.uniqueKey, record)

          Some(new InputEnvelope(
            s"${csvInputOptions.outputStream}$key",
            Array((csvInputOptions.outputStream, distributor.getNextPartition(Some(record)))),
            true,
            record))
        } else {
          buildFallbackEnvelope(line)
        }
      case Failure(_) => buildFallbackEnvelope(line)
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

  private def getPartitionCount(streamDomain: StreamDomain) = {
    streamDomain match {
      case s: TStreamStreamDomain => s.partitions
      case s: KafkaStreamDomain => s.partitions
      case _ => throw new IllegalArgumentException(s"stream type must be ${StreamLiterals.tstreamType} or " +
        s"${StreamLiterals.kafkaStreamType}")
    }
  }
}