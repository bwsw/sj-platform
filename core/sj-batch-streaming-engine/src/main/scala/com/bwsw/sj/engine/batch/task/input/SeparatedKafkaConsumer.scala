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
package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.engine.core.engine.input.KafkaConsumerWrapper
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}

/**
  * Retrieves records from Kafka using specific Kafka Consumer for each topic
  *
  * @param kafkaConsumerPerTopic Kafka consumers specified by topic
  * @author Pavel Tomskikh
  */
class SeparatedKafkaConsumer(kafkaConsumerPerTopic: Map[String, KafkaConsumer[Array[Byte], Array[Byte]]])
  extends KafkaConsumerWrapper {

  override def poll(timeout: Long, maybeTopic: Option[String] = None): ConsumerRecords[Array[Byte], Array[Byte]] = {
    maybeTopic match {
      case Some(topic) =>
        kafkaConsumerPerTopic.get(topic) match {
          case Some(consumer) =>
            consumer.poll(timeout)
          case None =>
            throw new IllegalArgumentException(s"KafkaConsumer for topic '$topic' not defined")
        }

      case None =>
        throw new IllegalArgumentException("Parameter 'topic' must be defined")
    }
  }

  override def close(): Unit =
    kafkaConsumerPerTopic.values.foreach(_.close())
}
