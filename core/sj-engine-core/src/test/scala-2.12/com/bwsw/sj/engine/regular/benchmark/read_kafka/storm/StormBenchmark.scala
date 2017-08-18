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
package com.bwsw.sj.engine.regular.benchmark.read_kafka.storm

import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.regular.RegularKafkaReaderBenchmark
import com.bwsw.sj.engine.regular.benchmark.read_kafka.storm.StormBenchmarkLiterals._

/**
  * Provides methods for testing the speed of reading data by [[http://storm.apache.org Apache Storm]] from Kafka.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * @param zooKeeperAddress ZooKeeper server's address. Must point to the ZooKeeper server that used by the Kafka server.
  * @param kafkaAddress     Kafka server's address
  * @param words            list of words that are sent to the Kafka server
  * @author Pavel Tomskikh
  */
class StormBenchmark(zooKeeperAddress: String,
                     kafkaAddress: String,
                     words: Array[String])
  extends RegularKafkaReaderBenchmark(zooKeeperAddress, kafkaAddress, words) {

  override protected def runProcess(messagesCount: Long): Process = {
    val properties = Map(
      kafkaTopicProperty -> kafkaTopic,
      outputFilenameProperty -> outputFile.getAbsolutePath,
      messagesCountProperty -> messagesCount.toString)

    new ClassRunner(classOf[StormBenchmarkLocalCluster], properties = properties).start()
  }
}
