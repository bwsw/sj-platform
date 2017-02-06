package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.common.utils.EngineLiterals

import scala.collection.JavaConverters._
import com.hazelcast.config._
import com.hazelcast.core.Hazelcast
import org.slf4j.LoggerFactory

/**
 * Provides methods are responsible for an eviction policy of input envelope duplicates
 *
 *
 * @param instance Input instance contains a settings of an eviction policy
 *                 (message TTL, a default eviction policy, a maximum size of message queue,
 *                 async and sync backup count)
 * @author Kseniya Mikhaleva
 */

abstract class InputInstanceEvictionPolicy(instance: InputInstance) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val hazelcastMapName = instance.name + "-" + "inputEngine"
  private val config = createHazelcastConfig()
  private val hazelcastInstance = Hazelcast.newHazelcastInstance(config)
  protected val uniqueEnvelopes = getUniqueEnvelopes

  /**
   * Checks whether a specific key is duplicate or not
   * @param key Key that will be checked
   * @param value In case there is a need to update duplicate key this value will be used
   * @return True if the key is not duplicate and false in other case
   */
  def checkForDuplication(key: String, value: Array[Byte]): Boolean

  /**
   * Returns a keys storage (Hazelcast map) for checking of there are duplicates (input envelopes) or not
   *
   * @return Storage of keys (Hazelcast map)
   */
  def getUniqueEnvelopes = {
    logger.debug(s"Get hazelcast map for checking of there are duplicates (input envelopes) or not.")
    hazelcastInstance.getMap[String, Array[Byte]](hazelcastMapName)
  }

  private def createHazelcastConfig() = {
    logger.debug(s"Create a Hazelcast map configuration is named '$hazelcastMapName'.")
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

  private def createEvictionPolicy() = {
    logger.debug(s"Create EvictionPolicy.")
    instance.defaultEvictionPolicy match {
      case EngineLiterals.lruDefaultEvictionPolicy => EvictionPolicy.LRU
      case EngineLiterals.lfuDefaultEvictionPolicy => EvictionPolicy.LFU
      case _ => EvictionPolicy.NONE
    }
  }

  private def createNetworkConfig(): NetworkConfig = {
    val networkConfig = new NetworkConfig()
    networkConfig.setJoin(createJoinConfig())
  }

  private def createJoinConfig() = {
    val joinConfig = new JoinConfig()
    joinConfig.setMulticastConfig(new MulticastConfig().setEnabled(false))
    joinConfig.setTcpIpConfig(createTcpIpConfig())
  }

  private def createTcpIpConfig() = {
    val tcpIpConfig = new TcpIpConfig()
    val hosts = System.getenv("INSTANCE_HOSTS").split(",").toList.asJava
    tcpIpConfig.setMembers(hosts).setEnabled(true)
  }

  /**
   * Creates a config that defines a max size of Hazelcast map
   *
   * @return Configuration for map's capacity.
   */
  private def createMaxSizeConfig() = {
    logger.debug(s"Create MaxSizeConfig.")
    new MaxSizeConfig()
      .setSize(instance.queueMaxSize)
  }
}

object InputInstanceEvictionPolicy {
  /**
   * Creates an eviction policy that defines a way of eviction of duplicate envelope
   * @return Eviction policy of duplicate envelopes
   */
  def apply(instance: InputInstance) = {
    instance.evictionPolicy match {
      case EngineLiterals.fixTimeEvictionPolicy => new FixTimeEvictionPolicy(instance)
      case EngineLiterals.expandedTimeEvictionPolicy => new ExpandedTimeEvictionPolicy(instance)
      case _ => throw new RuntimeException(s"There is no eviction policy named: ${instance.evictionPolicy}")
    }
  }
}