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
package com.bwsw.sj.engine.core.testutils.checkers

import com.bwsw.common.ObjectSerializer
import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.collection.JavaConverters._

/**
  * Returns a data from Kafka
  *
  * @author Pavel Tomskikh
  */
class KafkaReader[+T](consumer: KafkaConsumer[Array[Byte], Array[Byte]]) extends Reader[T] {

  /**
    * Returns a data from Kafka
    *
    * @return a data from Kafka
    */
  def get(): Seq[T] = {
    val objectSerializer = new ObjectSerializer()

    consumer.poll(1000 * 20).asScala.toList
      .map(_.value())
      .map(bytes => objectSerializer.deserialize(bytes).asInstanceOf[T])
  }
}