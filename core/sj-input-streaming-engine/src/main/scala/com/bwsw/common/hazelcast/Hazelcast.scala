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
package com.bwsw.common.hazelcast

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.input.config.InputEngineConfigNames
import com.hazelcast.config._
import com.hazelcast.core.{HazelcastInstance, IMap, Hazelcast => OriginalHazelcast}
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  * Wrapper for hazelcast cluster
  *
  * @param mapName      name of hazelcast map
  * @param configParams configuration parameters for hazelcast cluster
  * @author Pavel Tomskikh
  */
class Hazelcast(mapName: String, configParams: HazelcastConfig) extends HazelcastInterface {

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val config = createConfig(mapName)
  private val hazelcastInstance: HazelcastInstance = OriginalHazelcast.newHazelcastInstance(config)

  /**
    * @inheritdoc
    */
  override def getMap: IMap[String, String] =
    hazelcastInstance.getMap(mapName)

  private def createConfig(mapName: String): Config = {
    logger.debug(s"Create a hazelcast cluster configuration with map '$mapName'.")
    val config = new XmlConfigBuilder().build()
    val networkConfig = createNetworkConfig()
    val evictionPolicy = createEvictionPolicy()
    val maxSizeConfig = createMaxSizeConfig()

    config.setNetworkConfig(networkConfig)
      .getMapConfig(mapName)
      .setTimeToLiveSeconds(configParams.ttlSeconds)
      .setEvictionPolicy(evictionPolicy)
      .setMaxSizeConfig(maxSizeConfig)
      .setAsyncBackupCount(configParams.asyncBackupCount)
      .setBackupCount(configParams.backupCount)

    config
  }

  private def createEvictionPolicy(): EvictionPolicy = {
    logger.debug("Create a hazelcast eviction policy.")
    configParams.evictionPolicy match {
      case EngineLiterals.lruDefaultEvictionPolicy => EvictionPolicy.LRU
      case EngineLiterals.lfuDefaultEvictionPolicy => EvictionPolicy.LFU
      case _ => EvictionPolicy.NONE
    }
  }

  private def createNetworkConfig(): NetworkConfig = {
    logger.debug("Create a hazelcast network config.")
    val networkConfig = new NetworkConfig()
    networkConfig.setJoin(createJoinConfig())
  }

  private def createJoinConfig(): JoinConfig = {
    logger.debug("Create a hazelcast join config.")
    val joinConfig = new JoinConfig()
    joinConfig.setMulticastConfig(new MulticastConfig().setEnabled(false))
    joinConfig.setTcpIpConfig(createTcpIpConfig())
  }

  private def createTcpIpConfig(): TcpIpConfig = {
    logger.debug("Create a hazelcast tcp/ip config.")
    val tcpIpConfig = new TcpIpConfig()
    val config = ConfigFactory.load()
    val hosts = config.getString(InputEngineConfigNames.hosts).split(",").toList.asJava
    tcpIpConfig.setMembers(hosts).setEnabled(true)
  }

  /**
    * Creates a config that defines a max size of Hazelcast map
    *
    * @return Configuration for map's capacity.
    */
  private def createMaxSizeConfig(): MaxSizeConfig = {
    logger.debug("Create a hazelcast max size config.")
    new MaxSizeConfig()
      .setSize(configParams.maxSize)
  }
}

/**
  * Wrapper for hazelcast cluster
  */
trait HazelcastInterface {

  /**
    * Returns the hazelcast map
    */
  def getMap: IMap[String, String]
}
