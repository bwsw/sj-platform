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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.storm

import java.util.UUID

import com.bwsw.sj.common.utils.BenchmarkConfigNames.zooKeeperAddressConfig
import com.bwsw.sj.engine.batch.benchmark.read_kafka.storm.StormBenchmarkBatchLiterals._
import com.bwsw.sj.engine.regular.benchmark.read_kafka.storm.StormBenchmarkLiterals._
import com.typesafe.config.ConfigFactory
import org.apache.storm.kafka.{KafkaSpout, SpoutConfig, StringScheme, ZkHosts}
import org.apache.storm.spout.SchemeAsMultiScheme
import org.apache.storm.topology.TopologyBuilder
import org.apache.storm.topology.base.BaseWindowedBolt.Duration
import org.apache.storm.{Config, LocalCluster}

/**
  * @author Pavel Tomskikh
  */
object StormBenchmarkLocalCluster extends App {
  private val topic = System.getProperty(kafkaTopicProperty)
  private val messagesCount = System.getProperty(messagesCountProperty).toLong
  private val outputFilename = System.getProperty(outputFilenameProperty)
  private val batchSize = System.getProperty(batchSizeProperty).toInt
  private val windowSize = System.getProperty(windowSizeProperty).toInt
  private val slidingInterval = System.getProperty(slidingIntervalProperty).toInt

  private val typesafeConfig = ConfigFactory.load()
  private val zooKeeperAddress = typesafeConfig.getString(zooKeeperAddressConfig)

  private val zkHosts = new ZkHosts(zooKeeperAddress)
  private val spoutConfig = new SpoutConfig(zkHosts, topic, "/" + topic, UUID.randomUUID().toString)
  spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme)
  private val kafkaSpout = new KafkaSpout(spoutConfig)

  private val stormBenchmarkBolt = new StormBenchmarkBolt(messagesCount, outputFilename)

  private val spoutName = "kafka-spout"
  private val topologyBuilder = new TopologyBuilder
  topologyBuilder.setSpout(spoutName, kafkaSpout)
  topologyBuilder.setBolt(
    "kafka-bolt",
    stormBenchmarkBolt.withWindow(Duration.of(batchSize * windowSize), Duration.of(batchSize * slidingInterval)),
    1)
    .shuffleGrouping(spoutName)
  private val topology = topologyBuilder.createTopology()

  private val stormConfig = new Config
  stormConfig.setNumWorkers(1)
  stormConfig.setMaxTaskParallelism(1)
  stormConfig.setDebug(false)

  private val topologyName = "benchmark-topology"

  private val cluster = new LocalCluster()
  cluster.submitTopology(topologyName, stormConfig, topology)
}

class StormBenchmarkLocalCluster
