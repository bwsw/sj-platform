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
package com.bwsw.sj.engine.core.testutils.benchmark.sj

import java.util.Date

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ProviderLiterals

/**
  * Provides method to create an input stream and upload it into a Mongo storage
  *
  * @author Pavel Tomskikh
  */
class KafkaInputStreamFactory(override val name: String, kafkaAddress: String) extends InputStreamFactory {


  override def loadInputStream(benchmarkPreparation: SjBenchmarkHelper[_],
                               connectionRepository: ConnectionRepository): KafkaStreamDomain = {
    val provider = new ProviderDomain(
      name = "benchmark-kafka-provider",
      description = "Kafka provider for benchmark",
      hosts = Array(kafkaAddress),
      providerType = ProviderLiterals.kafkaType,
      creationDate = new Date())

    val service = new KafkaServiceDomain(
      name = "benchmark-kafka-service",
      description = "Kafka service for benchmark",
      provider = provider,
      zkProvider = benchmarkPreparation.zooKeeperProvider,
      zkNamespace = benchmarkPreparation.zkNamespace,
      creationDate = new Date())

    val stream = new KafkaStreamDomain(
      name = name,
      service = service,
      partitions = partitions,
      replicationFactor = 1,
      creationDate = new Date())

    connectionRepository.getProviderRepository.save(provider)
    connectionRepository.getServiceRepository.save(service)
    connectionRepository.getStreamRepository.save(stream)

    stream
  }
}
