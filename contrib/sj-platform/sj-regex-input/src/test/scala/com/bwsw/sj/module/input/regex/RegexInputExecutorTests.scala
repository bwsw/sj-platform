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

import java.nio.charset.Charset

import com.bwsw.common.JsonSerializer
import com.bwsw.common.hazelcast.HazelcastConfig
import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.InputStreamingResponse
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.stream_distributor.StreamDistributor
import com.bwsw.sj.engine.core.simulation.input.mocks.HazelcastMock
import com.bwsw.sj.engine.core.simulation.input.{InputEngineSimulator, OutputData}
import com.bwsw.sj.engine.input.eviction_policy.FixTimeEvictionPolicy
import com.bwsw.sj.module.input.regex.RegexInputOptionsNames.{fallbackFieldName, fallbackRecordName, firstMatchWinPolicy, outputRecordName}
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for [[RegexInputExecutor]]
  *
  * @author Pavel Tomskikh
  */
class RegexInputExecutorTests extends FlatSpec with Matchers with MockitoSugar {

  val fallbackStream = new TStreamStreamDomain("fallback-stream", mock[TStreamServiceDomain], 3)
  val outputStream1 = new TStreamStreamDomain("output-stream-1", mock[TStreamServiceDomain], 3)
  val outputStream2 = new TStreamStreamDomain("output-stream-2", mock[TStreamServiceDomain], 3)

  val field1Name = "field1"
  val field2Name = "field2"
  val field3Name = "field3"
  val defaultValue = "0"

  val rule1 = Rule(
    regex = s"""^(?<$field1Name>\w+),(?<$field2Name>\d+),(?<$field3Name>\w+)""",
    fields = List(
      Field(field1Name, defaultValue, "string"),
      Field(field2Name, defaultValue, "int"),
      Field(field3Name, defaultValue, "string")),
    outputStream = outputStream1.name,
    uniqueKey = List(field1Name, field3Name),
    distribution = List(field2Name))

  val rule2 = Rule(
    regex = s"""^(?<$field1Name>\d+),(?<$field2Name>\w+),(?<$field3Name>\d+)""",
    fields = List(
      Field(field1Name, defaultValue, "int"),
      Field(field2Name, defaultValue, "string"),
      Field(field3Name, defaultValue, "int")),
    outputStream = outputStream1.name,
    uniqueKey = List(field2Name),
    distribution = List(field3Name))

  val schema = SchemaBuilder.record(outputRecordName).fields()
    .name(field1Name).`type`().stringType().stringDefault(defaultValue)
    .name(field2Name).`type`().stringType().stringDefault(defaultValue)
    .name(field3Name).`type`().stringType().stringDefault(defaultValue)
    .endRecord()

  val fallbackAvroSchema = SchemaBuilder.record(fallbackRecordName).fields()
    .name(fallbackFieldName).`type`().stringType().noDefault()
    .endRecord()

  val options = RegexInputOptions(
    lineSeparator = "\n",
    policy = firstMatchWinPolicy,
    encoding = "UTF-8",
    fallbackStream = fallbackStream.name,
    rules = List(rule1, rule2))

  val serializer = new JsonSerializer
  val serializedOptions = serializer.serialize(options)
  val manager = new InputEnvironmentManager(serializedOptions, Array(outputStream1, outputStream2, fallbackStream))
  val hazelcastConfig = HazelcastConfig(600, 1, 1, EngineLiterals.lruDefaultEvictionPolicy, 100)


  trait TestPreparation {
    val executor = new RegexInputExecutor(manager)
    val hazelcast = new HazelcastMock(hazelcastConfig)
    val evictionPolicy = new FixTimeEvictionPolicy(hazelcast)
    val fallbackDistributor = new StreamDistributor(fallbackStream.partitions)

    val simulator = new InputEngineSimulator(
      executor,
      evictionPolicy,
      options.lineSeparator,
      Charset.forName(options.encoding))
  }


  case class CorrectInputData1(field1: String, field2: Int, field3: String, isNotDuplicate: Boolean = true)
    extends CorrectInputData(field1, field2, field3, isNotDuplicate) {

    override def key: String =
      s"${outputStream1.name},$field1,$field3"

    override def getPartitionByHash: Int =
      positiveMod(s"$field2".hashCode, outputStream1.partitions)

    override def toString: String = s"$field1,$field2,$field3"

    override def createInputEnvelope: InputEnvelope[Record] = {
      InputEnvelope(
        key,
        Seq((outputStream2.name, getPartitionByHash)),
        createAvroRecord,
        Some(true))
    }
  }


  case class CorrectInputData2(field1: Int, field2: String, field3: Int, isNotDuplicate: Boolean = true)
    extends CorrectInputData(field1, field2, field3, isNotDuplicate) {

    override def key: String =
      s"${outputStream2.name},$field2"

    override def getPartitionByHash: Int =
      positiveMod(s"$field3".hashCode, outputStream2.partitions)

    override def toString: String = s"$field1,$field2,$field3"

    override def createInputEnvelope: InputEnvelope[Record] = {
      InputEnvelope(
        key,
        Seq((outputStream1.name, getPartitionByHash)),
        createAvroRecord,
        Some(true))
    }
  }


  abstract class CorrectInputData(field1: Any, field2: Any, field3: Any, isNotDuplicate: Boolean = true) {
    def key: String

    def getPartitionByHash: Int

    def createInputEnvelope: InputEnvelope[Record]

    def createAvroRecord: Record = {
      val record = new Record(schema)
      record.put(field1Name, field1)
      record.put(field2Name, field2)
      record.put(field3Name, field3)

      record
    }

    def createOutputData: OutputData[Record] = {
      OutputData(
        Some(createInputEnvelope),
        Some(isNotDuplicate),
        createInputStreamingResponse(key, isNotDuplicate)
      )
    }

    protected def positiveMod(dividend: Int, divider: Int): Int = (dividend % divider + divider) % divider
  }


  case class IncorrectInputData(line: String, isNotDuplicate: Boolean = true) {

    override def toString: String = line

    def key: String = s"${fallbackStream.name},$line"

    def createAvroRecord: Record = {
      val record = new Record(fallbackAvroSchema)
      record.put(fallbackFieldName, line)

      record
    }

    def createInputEnvelope(partition: Int): InputEnvelope[Record] = {
      InputEnvelope(
        key,
        Seq((fallbackStream.name, partition)),
        createAvroRecord,
        None)
    }

    def createOutputData(partition: Int): OutputData[Record] = {
      OutputData(
        Some(createInputEnvelope(partition)),
        Some(isNotDuplicate),
        createInputStreamingResponse(key, isNotDuplicate))
    }
  }


  def createInputStreamingResponse(key: String, isNotDuplicate: Boolean): InputStreamingResponse = {
    if (isNotDuplicate) {
      InputStreamingResponse(
        s"Input envelope with key: '$key' has been sent\n",
        sendResponsesNow = false)
    } else {
      InputStreamingResponse(
        s"Input envelope with key: '$key' is duplicate\n",
        sendResponsesNow = true)
    }
  }

}
