package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.input.config.InputEngineConfigNames
import com.hazelcast.config._
import com.hazelcast.core.{Hazelcast, HazelcastInstance, IMap}
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  * Provides methods are responsible for an eviction policy of input envelope duplicates
  *
  * @param instance Input instance contains a settings of an eviction policy
  *                 (message TTL [[InputInstance.lookupHistory]],
  *                 a default eviction policy [[InputInstance.defaultEvictionPolicy]],
  *                 a maximum size of message queue [[InputInstance.queueMaxSize]],
  *                 async and sync backup count [[InputInstance.asyncBackupCount]] [[InputInstance.backupCount]])
  * @author Kseniya Mikhaleva
  */

abstract class InputInstanceEvictionPolicy(instance: InputInstance) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val stubValue: String = "stub"
  private val hazelcastMapName: String = instance.name + "-" + "inputEngine"
  private val config: Config = createHazelcastConfig()
  private val hazelcastInstance: HazelcastInstance = Hazelcast.newHazelcastInstance(config)
  protected val uniqueEnvelopes: IMap[String, String] = getUniqueEnvelopes

  /**
    * Checks whether a specific key is duplicate or not
    *
    * @param key Key that will be checked
    * @return True if the key is not duplicate and false in other case
    */
  def checkForDuplication(key: String): Boolean

  /**
    * Returns a keys storage (Hazelcast map) for checking of there are duplicates (input envelopes) or not
    *
    * @return Storage of keys (Hazelcast map)
    */
  def getUniqueEnvelopes: IMap[String, String] = {
    logger.debug(s"Get a hazelcast map for checking of there are duplicates (input envelopes) or not.")
    hazelcastInstance.getMap[String, String](hazelcastMapName)
  }

  private def createHazelcastConfig(): Config = {
    logger.debug(s"Create a hazelcast map configuration is named '$hazelcastMapName'.")
    val config = new XmlConfigBuilder().build()
    val networkConfig = createNetworkConfig()
    val evictionPolicy = createEvictionPolicy()
    val maxSizeConfig = createMaxSizeConfig()

    config.setNetworkConfig(networkConfig)
      .getMapConfig(hazelcastMapName)
      .setTimeToLiveSeconds(instance.lookupHistory)
      .setEvictionPolicy(evictionPolicy)
      .setMaxSizeConfig(maxSizeConfig)
      .setAsyncBackupCount(instance.asyncBackupCount)
      .setBackupCount(instance.backupCount)

    config
  }

  private def createEvictionPolicy(): EvictionPolicy = {
    logger.debug(s"Create a hazelcast eviction policy.")
    instance.defaultEvictionPolicy match {
      case EngineLiterals.lruDefaultEvictionPolicy => EvictionPolicy.LRU
      case EngineLiterals.lfuDefaultEvictionPolicy => EvictionPolicy.LFU
      case _ => EvictionPolicy.NONE
    }
  }

  private def createNetworkConfig(): NetworkConfig = {
    logger.debug(s"Create a hazelcast network config.")
    val networkConfig = new NetworkConfig()
    networkConfig.setJoin(createJoinConfig())
  }

  private def createJoinConfig(): JoinConfig = {
    logger.debug(s"Create a hazelcast join config.")
    val joinConfig = new JoinConfig()
    joinConfig.setMulticastConfig(new MulticastConfig().setEnabled(false))
    joinConfig.setTcpIpConfig(createTcpIpConfig())
  }

  private def createTcpIpConfig(): TcpIpConfig = {
    logger.debug(s"Create a hazelcast tcp/ip config.")
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
    logger.debug(s"Create a hazelcast max size config.")
    new MaxSizeConfig()
      .setSize(instance.queueMaxSize)
  }
}

object InputInstanceEvictionPolicy {
  /**
    * Creates an eviction policy that defines a way of eviction of duplicate envelope
    *
    * @return Eviction policy of duplicate envelopes
    */
  def apply(instance: InputInstance): InputInstanceEvictionPolicy = {
    instance.evictionPolicy match {
      case EngineLiterals.fixTimeEvictionPolicy => new FixTimeEvictionPolicy(instance)
      case EngineLiterals.expandedTimeEvictionPolicy => new ExpandedTimeEvictionPolicy(instance)
      case _ => throw new RuntimeException(s"There is no eviction policy named: ${instance.evictionPolicy}")
    }
  }
}