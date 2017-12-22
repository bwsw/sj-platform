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
package com.bwsw.sj.common.utils.benchmark

import com.typesafe.config.ConfigFactory

/**
  * Sends a sequence of numbers from <first> to <last> into Kafka topic
  *
  * Configuration:
  *
  * test.kafka.address - Kafka server's address. Environment variable KAFKA_ADDRESS.
  *
  * test.kafka.topic - Kafka topic. Environment variable KAFKA_TOPIC.
  *
  * test.kafka.first - <first> (1 by default). Environment variable FIRST.
  *
  * test.kafka.last - <last>. Environment variable LAST.
  *
  * @author Pavel Tomskikh
  */
object KafkaNumberSequenceSender extends App {
  private val config = ConfigFactory.load()
  private val kafkaAddress = config.getString("test.kafka.address")
  private val topic = config.getString("test.kafka.topic")
  private val first = config.getInt("test.kafka.first")
  private val last = config.getInt("test.kafka.last")
  private val sender = new KafkaDataSender(kafkaAddress)

  sender.send((first to last).map(Int.box), topic)
}
