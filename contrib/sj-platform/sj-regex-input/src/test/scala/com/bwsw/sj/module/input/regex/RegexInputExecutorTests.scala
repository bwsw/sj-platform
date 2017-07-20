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
import java.util.Date

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.FileStorage
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
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.{Schema, SchemaBuilder}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for [[RegexInputExecutor]]
  *
  * @author Pavel Tomskikh
  */
class RegexInputExecutorTests extends FlatSpec with Matchers with MockitoSugar {

  val fallbackStream = new TStreamStreamDomain("fallback-stream", mock[TStreamServiceDomain], 3, creationDate = new Date())
  val outputStream1 = new TStreamStreamDomain("output-stream-1", mock[TStreamServiceDomain], 3, creationDate = new Date())
  val outputStream2 = new TStreamStreamDomain("output-stream-2", mock[TStreamServiceDomain], 3, creationDate = new Date())

  val field1Name = "field1"
  val field2Name = "field2"
  val field3Name = "field3"
  val defaultStringValue = "default"
  val defaultIntValue = 0

  val rule1 = Rule(
    regex = s"^(?<$field1Name>\\w+),(?<$field2Name>\\d+),(?<$field3Name>\\w+)",
    fields = List(
      Field(field1Name, defaultStringValue, "string"),
      Field(field2Name, defaultIntValue.toString, "int"),
      Field(field3Name, defaultStringValue, "string")),
    outputStream = outputStream1.name,
    uniqueKey = List(field1Name, field3Name),
    distribution = List(field2Name))

  val rule2 = Rule(
    regex = s"^(?<$field1Name>\\d+),(?<$field2Name>\\w+),(?<$field3Name>\\d+)",
    fields = List(
      Field(field1Name, defaultIntValue.toString, "int"),
      Field(field2Name, defaultStringValue, "string"),
      Field(field3Name, defaultIntValue.toString, "int")),
    outputStream = outputStream2.name,
    uniqueKey = List(field2Name),
    distribution = List(field3Name))

  val schema1 = SchemaBuilder.record(outputRecordName).fields()
    .name(field1Name).`type`().stringType().stringDefault(defaultStringValue)
    .name(field2Name).`type`().intType().intDefault(defaultIntValue)
    .name(field3Name).`type`().stringType().stringDefault(defaultStringValue)
    .endRecord()

  val schema2 = SchemaBuilder.record(outputRecordName).fields()
    .name(field1Name).`type`().intType().intDefault(defaultIntValue)
    .name(field2Name).`type`().stringType().stringDefault(defaultStringValue)
    .name(field3Name).`type`().intType().intDefault(defaultIntValue)
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
  val manager = new InputEnvironmentManager(
    serializedOptions,
    Array(outputStream1, outputStream2, fallbackStream),
    mock[FileStorage])
  val hazelcastConfig = HazelcastConfig(600, 1, 1, EngineLiterals.lruDefaultEvictionPolicy, 100, Seq("localhost"))


  "RegexInputExecutor" should "handle correct input data properly" in new TestPreparation {
    val duplicatedField1Value = "duplicatedField1"
    val duplicatedField2Value = "duplicatedField2"
    val duplicatedField3Value = "duplicatedField3"

    val correctInputDataList = Seq(
      CorrectInputData1("value11", 1, "value13"),
      CorrectInputData2(1, "value12", 10),
      CorrectInputData1(duplicatedField1Value, 101, duplicatedField3Value),
      CorrectInputData1("value21", 2, "value23"),
      CorrectInputData2(201, duplicatedField2Value, 301),
      CorrectInputData2(2, "value22", 20),
      CorrectInputData1("value31", 3, "value33"),
      CorrectInputData2(202, duplicatedField2Value, 302, isNotDuplicate = false),
      CorrectInputData1(duplicatedField1Value, 102, duplicatedField3Value, isNotDuplicate = false),
      CorrectInputData2(203, duplicatedField2Value, 303, isNotDuplicate = false),
      CorrectInputData1(duplicatedField1Value, 103, duplicatedField3Value, isNotDuplicate = false),
      CorrectInputData2(3, "value32", 30))

    simulator.prepare(correctInputDataList.map(_.toString))

    val expectedOutputDataList = correctInputDataList.map(_.createOutputData)
    val outputDataList = simulator.process(duplicateCheck = false)
    // In this case value of duplicateCheck does not have any effect because
    // the RegexInputExecutor forces envelopes to be checked on duplicate

    outputDataList shouldBe expectedOutputDataList
  }


  val duplicatedIncorrectLine = "duplicated line that does not matches"

  it should "handle incorrect input data properly (default duplication checking is disabled)" in new TestPreparation {
    val incorrectInputDataList = Seq(
      IncorrectInputData("line that does not matches 1"),
      IncorrectInputData(duplicatedIncorrectLine),
      IncorrectInputData("line that does not matches 2"),
      IncorrectInputData(duplicatedIncorrectLine),
      IncorrectInputData("line that does not matches 3"),
      IncorrectInputData(duplicatedIncorrectLine))

    simulator.prepare(incorrectInputDataList.map(_.toString))

    val expectedOutputDataList = incorrectInputDataList.map(_.createOutputData(fallbackDistributor.getNextPartition()))
    val outputDataList = simulator.process(duplicateCheck = false)

    outputDataList shouldBe expectedOutputDataList
  }

  it should "handle incorrect input data properly (default duplication checking is enabled)" in new TestPreparation {
    val incorrectInputDataList = Seq(
      IncorrectInputData("line that does not matches 1"),
      IncorrectInputData(duplicatedIncorrectLine),
      IncorrectInputData("line that does not matches 2"),
      IncorrectInputData(duplicatedIncorrectLine, isNotDuplicate = false),
      IncorrectInputData("line that does not matches 3"),
      IncorrectInputData(duplicatedIncorrectLine, isNotDuplicate = false))

    simulator.prepare(incorrectInputDataList.map(_.toString))

    val expectedOutputDataList = incorrectInputDataList.map(_.createOutputData(fallbackDistributor.getNextPartition()))
    val outputDataList = simulator.process(duplicateCheck = true)

    outputDataList shouldBe expectedOutputDataList
  }


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
    extends CorrectInputData(field1, field2, field3, schema1, isNotDuplicate) {

    override def key: String =
      s"${outputStream1.name},$field1,$field3"

    override def getPartitionByHash: Int =
      positiveMod(s"$field2".hashCode, outputStream1.partitions)

    override def toString: String = s"$field1,$field2,$field3"

    override def createInputEnvelope: InputEnvelope[Record] = {
      InputEnvelope(
        key,
        Seq((outputStream1.name, getPartitionByHash)),
        createAvroRecord,
        Some(true))
    }
  }


  case class CorrectInputData2(field1: Int, field2: String, field3: Int, isNotDuplicate: Boolean = true)
    extends CorrectInputData(field1, field2, field3, schema2, isNotDuplicate) {

    override def key: String =
      s"${outputStream2.name},$field2"

    override def getPartitionByHash: Int =
      positiveMod(s"$field3".hashCode, outputStream2.partitions)

    override def toString: String = s"$field1,$field2,$field3"

    override def createInputEnvelope: InputEnvelope[Record] = {
      InputEnvelope(
        key,
        Seq((outputStream2.name, getPartitionByHash)),
        createAvroRecord,
        Some(true))
    }
  }


  abstract class CorrectInputData(field1: Any,
                                  field2: Any,
                                  field3: Any,
                                  schema: Schema,
                                  isNotDuplicate: Boolean) {
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
