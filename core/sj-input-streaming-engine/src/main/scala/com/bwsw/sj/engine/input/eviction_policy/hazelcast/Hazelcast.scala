package com.bwsw.sj.engine.input.eviction_policy.hazelcast

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.input.config.InputEngineConfigNames
import com.hazelcast.config._
import com.hazelcast.core.{HazelcastInstance, IMap, Hazelcast => OriginalHazelcast}
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  * Wrapper for hazelcast map
  *
  * @param mapName name of hazelcast map
  * @param params  parameters for hazelcast map
  * @author Pavel Tomskikh
  */
class Hazelcast(mapName: String, params: HazelcastParameters) extends HazelcastInterface {

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val config = createHazelcastConfig(mapName)
  private val hazelcastInstance: HazelcastInstance = OriginalHazelcast.newHazelcastInstance(config)

  override def getMap: IMap[String, String] =
    hazelcastInstance.getMap(mapName)

  private def createHazelcastConfig(mapName: String): Config = {
    logger.debug(s"Create a hazelcast map configuration is named '$mapName'.")
    val config = new XmlConfigBuilder().build()
    val networkConfig = createNetworkConfig()
    val evictionPolicy = createEvictionPolicy()
    val maxSizeConfig = createMaxSizeConfig()

    config.setNetworkConfig(networkConfig)
      .getMapConfig(mapName)
      .setTimeToLiveSeconds(params.lookupHistory)
      .setEvictionPolicy(evictionPolicy)
      .setMaxSizeConfig(maxSizeConfig)
      .setAsyncBackupCount(params.asyncBackupCount)
      .setBackupCount(params.backupCount)

    config
  }

  private def createEvictionPolicy(): EvictionPolicy = {
    logger.debug("Create a hazelcast eviction policy.")
    params.defaultEvictionPolicy match {
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
      .setSize(params.queueMaxSize)
  }
}

/**
  * Wrapper for hazelcast map
  */
trait HazelcastInterface {

  /**
    * Returns the hazelcast map
    */
  def getMap: IMap[String, String]
}
