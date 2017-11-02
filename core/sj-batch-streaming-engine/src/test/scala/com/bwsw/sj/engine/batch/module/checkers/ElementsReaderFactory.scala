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
package com.bwsw.sj.engine.batch.module.checkers

import com.bwsw.sj.engine.batch.module.DataFactory
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants.{inputCount, partitions}
import com.bwsw.sj.engine.core.testutils.checkers.{KafkaReader, TStreamsReader}
import com.bwsw.tstreams.agents.consumer.Consumer

/**
  * Provides methods to create data readers
  *
  * @author Pavel Tomskikh
  */
object ElementsReaderFactory {
  def createTStreamsInputElementsReader: TStreamsReader[Int] = {
    new TStreamsReader[Int]((1 to inputCount).map(x =>
      DataFactory.createInputTstreamConsumer(partitions, x.toString)))
  }

  def createKafkaInputElementsReader: KafkaReader[Int] =
    new KafkaReader[Int](DataFactory.createInputKafkaConsumer(inputCount, partitions))

  def createStateConsumer: Consumer =
    DataFactory.createStateConsumer(DataFactory.connectionRepository.getStreamRepository)

  def createOutputElementsReader: OutputReader = new OutputReader
}
