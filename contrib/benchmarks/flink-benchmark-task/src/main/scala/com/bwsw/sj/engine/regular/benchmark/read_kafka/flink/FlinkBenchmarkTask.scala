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
package com.bwsw.sj.engine.regular.benchmark.read_kafka.flink

import java.io.{File, FileWriter}
import java.util.UUID

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.util.serialization.SimpleStringSchema

import scala.collection.JavaConverters._

/**
  * @author Pavel Tomskikh
  */
object FlinkBenchmarkTask extends App {
  implicit val typeInfoString = TypeInformation.of(classOf[String])
  implicit val typeInfoUnit = TypeInformation.of(classOf[Unit])
  implicit val typeInfoLong = TypeInformation.of(classOf[Long])
  implicit val typeInfoOpt = TypeInformation.of(classOf[Option[Long]])

  val environment = StreamExecutionEnvironment.getExecutionEnvironment
  environment.setParallelism(1)

  val parameterTool = ParameterTool.fromArgs(args)
  val messagesCount = parameterTool.getRequired("messagesCount").toLong
  val outputFilename = parameterTool.getRequired("outputFile")
  val properties = parameterTool.getProperties
  properties.setProperty("auto.offset.reset", "earliest")
  properties.setProperty("group.id", UUID.randomUUID().toString)

  val consumer: FlinkKafkaConsumer010[String] = new FlinkKafkaConsumer010(
    Seq(parameterTool.getRequired("topic")).asJava, new SimpleStringSchema(), properties)
  val dataStream: DataStream[String] = environment.addSource(consumer)

  var processedMessages: Long = 0
  var firstMessageTimestamp: Long = 0

  dataStream.map { s: String =>
    if (processedMessages == 0)
      firstMessageTimestamp = System.currentTimeMillis()

    processedMessages += 1

    if (processedMessages == messagesCount) {
      val lastMessageTimestamp = System.currentTimeMillis()

      val outputFile = new File(outputFilename)
      val writer = new FileWriter(outputFile)
      writer.write(s"${lastMessageTimestamp - firstMessageTimestamp}\n")
      writer.close()
    }
  }

  environment.execute("flink-benchmark-job")
}
