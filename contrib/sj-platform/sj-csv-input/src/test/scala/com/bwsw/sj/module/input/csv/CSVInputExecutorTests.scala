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
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for [[CSVInputExecutor]]
  *
  * @author Pavel Tomskikh
  */
class CSVInputExecutorTests extends FlatSpec with Matchers with MockitoSugar {
  val outputStream = new TStreamStreamDomain("output-stream", mock[TStreamServiceDomain], 4)
  val fallbackStream = new TStreamStreamDomain("fallback-stream", mock[TStreamServiceDomain], 3)

  val key1Field = "key1"
  val key2Field = "key2"
  val uniqueKey = List(key1Field, key2Field)

  val otherField1Name = "otherField1"
  val otherField2Name = "otherField2"
  val fields = uniqueKey ++ List(otherField1Name, otherField2Name)
  val distribution = List(key1Field, otherField2Name)

  val options = CSVInputOptions(
    lineSeparator = "\n",
    encoding = "UTF-8",
    outputStream = outputStream.name,
    fallbackStream = fallbackStream.name,
    fields = fields,
    fieldSeparator = None,
    quoteSymbol = None,
    uniqueKey = uniqueKey,
    distribution = distribution)

  val serializer = new JsonSerializer
  val serializedOptions = serializer.serialize(options)

  val manager = new InputEnvironmentManager(serializedOptions, Array(outputStream, fallbackStream))
  val hazelcastConfig = HazelcastConfig(600, 1, 1, EngineLiterals.lruDefaultEvictionPolicy, 100, Seq("localhost"))

  val avroSchema = {
    var schemaBuilder = SchemaBuilder.record("csv").fields()
    options.fields.foreach { field =>
      schemaBuilder = schemaBuilder.name(field).`type`().stringType().noDefault()
    }
    schemaBuilder.endRecord()
  }

  val fallbackFieldName = "data"
  val fallbackAvroSchema = SchemaBuilder.record("fallback").fields()
    .name(fallbackFieldName).`type`().stringType().noDefault()
    .endRecord()


  "CSVInputExecutor" should "handle correct input data properly (default duplication checking disabled)" in new TestPreparation {
    val duplicatedKey1 = "duplicated-key-1"
    val duplicatedKey2 = "duplicated-key-2"

    val correctInputDataList = Seq(
      CorrectInputData(s"$key1Field-value1", s"$key2Field-value1", s"$otherField1Name-value1", s"$otherField2Name-value1"),
      CorrectInputData(s"$duplicatedKey1", s"$duplicatedKey2", s"$otherField1Name-original", s"$otherField2Name-original"),
      CorrectInputData(s"$duplicatedKey1", s"$duplicatedKey2", s"$otherField1Name-duplicate1", s"$otherField2Name-duplicate1", isNotDuplicate = false),
      CorrectInputData(s"$key1Field-value2", s"$key2Field-value2", s"$otherField1Name-value2", s"$otherField2Name-value2"),
      CorrectInputData(s"$duplicatedKey1", s"$duplicatedKey2", s"$otherField1Name-duplicate2", s"$otherField2Name-duplicate2", isNotDuplicate = false),
      CorrectInputData(s"$key1Field-value3", s"$key2Field-value3", s"$otherField1Name-value3", s"$otherField2Name-value3"),
      CorrectInputData(s"$duplicatedKey1", s"$duplicatedKey2", s"$otherField1Name-duplicate3", s"$otherField2Name-duplicate3", isNotDuplicate = false),
      CorrectInputData(s"$key1Field-value4", s"$key2Field-value4", s"$otherField1Name-value4", s"$otherField2Name-value4"))

    val expectedOutputDataList = correctInputDataList.map(_.createOutputData)

    simulator.prepare(correctInputDataList.map(_.toString))
    val outputDataList = simulator.process(duplicateCheck = false)
    // In this case value of duplicateCheck does not have any effect because
    // the CSVInputExecutor forces envelopes to be checked on duplicate

    outputDataList shouldBe expectedOutputDataList
  }


  val duplicatedIncorrectLine = "duplicated incorrect line"

  it should "handle incorrect input lines properly (default duplication checking disabled)" in new TestPreparation {
    val incorrectInputLines = Seq(
      IncorrectInputData("incorrect line 1"),
      IncorrectInputData("\""),
      IncorrectInputData(duplicatedIncorrectLine),
      IncorrectInputData(duplicatedIncorrectLine),
      IncorrectInputData("incorrect,line,2"),
      IncorrectInputData(duplicatedIncorrectLine),
      IncorrectInputData("incorrect line 3"))

    val expectedOutputDataList = incorrectInputLines.map(_.createOutputData(fallbackDistributor.getNextPartition()))

    simulator.prepare(incorrectInputLines.map(_.toString))
    val outputDataList = simulator.process(duplicateCheck = false)

    outputDataList shouldBe expectedOutputDataList
  }

  it should "handle incorrect input lines properly (default duplication checking enabled)" in new TestPreparation {
    val incorrectInputLines = Seq(
      IncorrectInputData(duplicatedIncorrectLine),
      IncorrectInputData("incorrect,line,1"),
      IncorrectInputData(duplicatedIncorrectLine, isNotDuplicate = false),
      IncorrectInputData("incorrect line 2"),
      IncorrectInputData("\""),
      IncorrectInputData(duplicatedIncorrectLine, isNotDuplicate = false))

    val expectedOutputDataList = incorrectInputLines.map(_.createOutputData(fallbackDistributor.getNextPartition()))

    simulator.prepare(incorrectInputLines.map(_.toString))
    val outputDataList = simulator.process(duplicateCheck = true)

    outputDataList shouldBe expectedOutputDataList
  }


  trait TestPreparation {
    val executor = new CSVInputExecutor(manager)
    val hazelcast = new HazelcastMock(hazelcastConfig)
    val evictionPolicy = new FixTimeEvictionPolicy(hazelcast)
    val fallbackDistributor = new StreamDistributor(fallbackStream.partitions)

    val simulator = new InputEngineSimulator(
      executor,
      evictionPolicy,
      options.lineSeparator,
      Charset.forName(options.encoding))
  }


  case class CorrectInputData(key1: String,
                              key2: String,
                              otherField1: String,
                              otherField2: String,
                              isNotDuplicate: Boolean = true) {

    override def toString: String =
      s"$key1,$key2,$otherField1,$otherField2"

    def key: String = s"${outputStream.name},$key1,$key2"

    def createAvroRecord: Record = {
      val record = new Record(avroSchema)
      record.put(key1Field, key1)
      record.put(key2Field, key2)
      record.put(otherField1Name, otherField1)
      record.put(otherField2Name, otherField2)

      record
    }

    def createInputEnvelope: InputEnvelope[Record] = {
      InputEnvelope(
        key,
        Seq((outputStream.name, getPartitionByHash)),
        createAvroRecord,
        Some(true))
    }

    def getPartitionByHash: Int = {
      val hash = s"$key1,$otherField2".hashCode

      (hash % outputStream.partitions + outputStream.partitions) % outputStream.partitions
    }

    def createOutputData: OutputData[Record] =
      OutputData(
        Some(createInputEnvelope),
        Some(isNotDuplicate),
        createInputStreamingResponse(key, isNotDuplicate))
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

    def createOutputData(partition: Int): OutputData[Record] =
      OutputData(
        Some(createInputEnvelope(partition)),
        Some(isNotDuplicate),
        createInputStreamingResponse(key, isNotDuplicate))
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
