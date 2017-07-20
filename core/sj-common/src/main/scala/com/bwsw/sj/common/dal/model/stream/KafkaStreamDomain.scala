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
package com.bwsw.sj.common.dal.model.stream

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import kafka.common.TopicAlreadyMarkedForDeletionException

import scala.util.{Failure, Success, Try}

class KafkaStreamDomain(override val name: String,
                        override val service: KafkaServiceDomain,
                        val partitions: Int,
                        val replicationFactor: Int,
                        override val description: String = RestLiterals.defaultDescription,
                        override val force: Boolean = false,
                        override val tags: Array[String] = Array(),
                        private val zkSessionTimeout: Int = ConfigLiterals.zkSessionTimeoutDefault)
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.kafkaType) {

  protected def createClient(): KafkaClient = new KafkaClient(this.service.zkProvider.hosts)

  override def create(): Unit = {
    Try {
      val client = createClient()
      if (!client.topicExists(this.name)) {
        client.createTopic(this.name, this.partitions, this.replicationFactor)
      }

      client.close()
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot create a kafka topic ${this.name}. Topic is marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }
}