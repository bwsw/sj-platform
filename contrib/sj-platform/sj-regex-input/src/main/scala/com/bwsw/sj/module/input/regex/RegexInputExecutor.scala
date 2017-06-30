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
package com.bwsw.sj.module.input.regex

import java.util.regex.Pattern

import com.bwsw.common.{AvroSerializer, JsonSerializer}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.utils.Tokenizer
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import com.bwsw.sj.common.utils.stream_distributor.{ByHash, StreamDistributor}
import com.bwsw.sj.common.utils.{AvroRecordUtils, StreamLiterals}
import io.netty.buffer.ByteBuf
import org.apache.avro.SchemaBuilder.FieldAssembler
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecord
import org.apache.avro.{Schema, SchemaBuilder}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Implementation of Input Streaming Executor for Regex Input Module
  *
  * @param manager Instance of InputEnvironmentManager used for receiving module's options
  * @author Ruslan Komarov
  */
class RegexInputExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Record](manager) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsonSerializer = new JsonSerializer
  private val avroSerializer = new AvroSerializer
  private val regexInputOptions = jsonSerializer.deserialize[RegexInputOptions](manager.options)

  private val outputSchemas = regexInputOptions.rules.map(r => r -> createOutputSchema(r.fields)).toMap
  private val outputDistributors = regexInputOptions.rules.map(r => r -> createOutputDistributor(r)).toMap

  private val fallbackSchema = SchemaBuilder.record(RegexInputOptionsNames.fallbackRecordName).fields()
    .name(RegexInputOptionsNames.fallbackFieldName).`type`().stringType().noDefault().endRecord()

  private val fallbackPartitionCount = getPartitionCount(manager.outputs.find(_.name == regexInputOptions.fallbackStream).get)
  private val fallbackDistributor = new StreamDistributor(fallbackPartitionCount)

  private val tokenizer = new Tokenizer(regexInputOptions.lineSeparator, regexInputOptions.encoding)

  private val policyHandler: String => Option[InputEnvelope[Record]] = regexInputOptions.policy match {
    case RegexInputOptionsNames.checkEveryPolicy => handleDataWithCheckEveryPolicy
    case RegexInputOptionsNames.firstMatchWinPolicy => handleDataWithFirstMatchWinPolicy
    case _ => throw new IllegalArgumentException(s"Incorrect or unsupported policy: ${regexInputOptions.policy}")
  }

  logger.info(s"Started with ${regexInputOptions.policy} policy")

  /**
    * Tokenize method implementation (uses SeparateTokenizer)
    *
    * @param buffer received data
    * @return Interval of ByteBuf that contains data (in bytes)
    */
  override def tokenize(buffer: ByteBuf): Option[Interval] = tokenizer.tokenize(buffer)

  /**
    * Parse method implementation. Receive data from tokenize method, parse it and pass on the output stream
    *
    * @param buffer   received data
    * @param interval Interval of buffer received from tokenize method
    * @return Option of InputEnvelop with data converted to Avro record
    */
  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Record]] = {
    val length = interval.finalValue - interval.initialValue
    val dataBuffer = buffer.slice(interval.initialValue, length)
    val data = new Array[Byte](length)

    dataBuffer.getBytes(0, data)
    buffer.readerIndex(interval.finalValue + 1)

    val line = Source.fromBytes(data, regexInputOptions.encoding).mkString

    logger.info(s"Received data $line")

    policyHandler(line)
  }

  override def serialize(obj: AnyRef): Array[Byte] =
    avroSerializer.serialize(obj.asInstanceOf[GenericRecord])

  private def handleDataWithCheckEveryPolicy(data: String): Option[InputEnvelope[Record]] = {
    // TODO: Change behavior for check-every policy
    logger.warn(s"${regexInputOptions.policy} policy is not implemented yet")
    buildFallbackEnvelope(data)
  }

  private def handleDataWithFirstMatchWinPolicy(data: String): Option[InputEnvelope[Record]] = {
    @tailrec
    def handleByRules(rulesList: List[Rule]): Option[InputEnvelope[Record]] = {
      rulesList match {
        case Nil =>
          logger.debug(s"Data $data was not match with all regex in rules list")
          buildFallbackEnvelope(data)

        case r :: rs if data.matches(r.regex) =>
          logger.debug(s"Data $data matched with regex ${r.regex}")
          buildOutputEnvelope(data, r)

        case r :: rs =>
          logger.debug(s"Data $data was not match with regex ${r.regex}")
          handleByRules(rs)
      }
    }

    handleByRules(regexInputOptions.rules)
  }

  private def buildOutputEnvelope(data: String, rule: Rule) = {
    logger.debug(s"Create input envelope: convert received data $data to Avro format using rule: $rule")

    val uniqueKey =
      if (rule.uniqueKey.nonEmpty) rule.uniqueKey
      else rule.fields.map(_.name)

    val ruleMatcher = Pattern.compile(rule.regex).matcher(data)
    val record = new Record(outputSchemas(rule))

    // Used to find the match in the data using the regex pattern
    if (ruleMatcher.find()) {
      rule.fields.foreach { field =>
        val fieldValue = Try[String](ruleMatcher.group(field.name)) match {
          case Success(value) => value
          case Failure(_) => field.defaultValue
        }
        record.put(field.name, fieldValue)
      }
    }

    logger.debug(s"Created Avro record from data: $record")

    val key = AvroRecordUtils.concatFields(uniqueKey, record)

    Some(InputEnvelope(
      s"${rule.outputStream},$key",
      Seq((rule.outputStream, outputDistributors(rule).getNextPartition(Some(record)))),
      record,
      Some(true)))
  }

  private def buildFallbackEnvelope(data: String): Option[InputEnvelope[Record]] = {
    logger.debug(s"Create input envelope for fallback stream from data: $data")
    val record = new Record(fallbackSchema)
    record.put(RegexInputOptionsNames.fallbackFieldName, data)

    Some(InputEnvelope(
      s"${regexInputOptions.fallbackStream},$data",
      Seq((regexInputOptions.fallbackStream, fallbackDistributor.getNextPartition())),
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

  private def createOutputSchema(fieldList: List[Field]) = {
    @tailrec
    def createSchemaInner(fieldList: List[Field], scheme: FieldAssembler[Schema]): Schema = {
      fieldList match {
        case Nil => scheme.endRecord()
        case f :: fs => createSchemaInner(fs, scheme.name(f.name).`type`().stringType().stringDefault(f.defaultValue))
      }
    }

    createSchemaInner(fieldList, SchemaBuilder.record(RegexInputOptionsNames.outputRecordName).fields())
  }

  private def createOutputDistributor(rule: Rule) = {
    val outputPartitionCount = getPartitionCount(manager.outputs.find(_.name == rule.outputStream).get)

    if (rule.distribution.isEmpty) new StreamDistributor(outputPartitionCount)
    else new StreamDistributor(outputPartitionCount, ByHash, rule.distribution)
  }
}