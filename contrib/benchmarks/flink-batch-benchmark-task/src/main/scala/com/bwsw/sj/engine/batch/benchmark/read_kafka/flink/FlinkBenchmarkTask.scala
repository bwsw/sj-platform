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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.flink

import java.io.{File, FileWriter}

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.util.Collector

/**
  * @author Pavel Tomskikh
  */
object FlinkBenchmarkTask extends App {
  implicit val typeInfoString = TypeInformation.of(classOf[String])
  implicit val typeInfoUnit = TypeInformation.of(classOf[Unit])
  implicit val typeInfoLong = TypeInformation.of(classOf[Long])
  implicit val typeInfoOpt = TypeInformation.of(classOf[Option[Long]])
  implicit val typeInfoStringCollection = TypeInformation.of(classOf[Iterable[String]])

  val environment = StreamExecutionEnvironment.getExecutionEnvironment
  environment.setParallelism(1)

  val messagesCountConfig = "messagesCount"
  val outputFileConfig = "outputFile"
  val batchSizeConfig = "batchSize"
  val windowSizeConfig = "windowSize"
  val slidingIntervalConfig = "slidingInterval"

  val parameterTool = ParameterTool.fromArgs(args)
  val messagesCount = parameterTool.getRequired(messagesCountConfig).toLong
  val outputFilename = parameterTool.getRequired(outputFileConfig)
  val batchSize = parameterTool.getRequired(batchSizeConfig).toLong
  val windowSize = parameterTool.getRequired(windowSizeConfig).toInt
  val slidingInterval = parameterTool.getRequired(slidingIntervalConfig).toInt

  val properties = parameterTool.getProperties
  properties.setProperty("auto.offset.reset", "earliest")

  val consumer: FlinkKafkaConsumer010[String] = new FlinkKafkaConsumer010(
    parameterTool.getRequired("topic"), new SimpleStringSchema(), properties)

  consumer.setStartFromEarliest()
  val dataStream: DataStream[String] = environment.addSource(consumer)

  var processedMessages: Long = 0
  var firstMessageTimestamp: Long = 0
  var done: Boolean = false

  val flinkBatchSize = batchSize * slidingInterval
  val flinkWindowSize = windowSize / slidingInterval

  dataStream
    .timeWindowAll(Time.milliseconds(flinkBatchSize))
    .apply((_, messages, collector: Collector[Iterable[String]]) => collector.collect(messages))
    .countWindowAll(flinkWindowSize, 1)
    .apply { (_, batches, _: Collector[Unit]) => {
      if (processedMessages == 0) {
        firstMessageTimestamp = System.currentTimeMillis()

        batches
      } else
        batches.takeRight(1)
    }.foreach(messages => processedMessages += messages.size)
      if (processedMessages >= messagesCount && !done) {
        val lastMessageTimestamp = System.currentTimeMillis()

        val outputFile = new File(outputFilename)
        val writer = new FileWriter(outputFile)
        writer.write(s"${lastMessageTimestamp - firstMessageTimestamp}\n")
        writer.close()

        done = true
      }
    }

  environment.execute("flink-benchmark-job")
}
