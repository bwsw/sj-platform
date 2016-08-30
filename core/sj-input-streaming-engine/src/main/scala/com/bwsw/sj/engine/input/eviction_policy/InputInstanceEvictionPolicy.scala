package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.hazelcast.config.{EvictionPolicy, MaxSizeConfig, XmlConfigBuilder}
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
  private val hazelcastMapName = "inputEngine"
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
    logger.debug(s"Get hazelcast map for checking of there are duplicates (input envelopes) or not\n")
    hazelcastInstance.getMap[String, Array[Byte]](hazelcastMapName)
  }

  private def createHazelcastConfig() = {
    logger.debug(s"Create a Hazelcast map configuration is named '$hazelcastMapName'\n")
    val config = new XmlConfigBuilder().build()
    val evictionPolicy = createEvictionPolicy()
    val maxSizeConfig = createMaxSizeConfig()

    config.getMapConfig(hazelcastMapName)
      .setTimeToLiveSeconds(instance.lookupHistory)
      .setEvictionPolicy(evictionPolicy)
      .setMaxSizeConfig(maxSizeConfig)
      .setAsyncBackupCount(instance.asyncBackupCount)
      .setBackupCount(instance.backupCount)

    config
  }

  private def createEvictionPolicy() = {
    logger.debug(s"Create EvictionPolicy\n")
    instance.defaultEvictionPolicy match {
      case "LRU" => EvictionPolicy.LRU
      case "LFU" => EvictionPolicy.LFU
      case _ => EvictionPolicy.NONE
    }
  }

  /**
   * Creates a config that defines a max size of Hazelcast map
   *
   * @return Configuration for map's capacity.
   */
  private def createMaxSizeConfig() = {
    logger.debug(s"Create MaxSizeConfig\n")
    new MaxSizeConfig()
      .setSize(instance.queueMaxSize)
  }
}
