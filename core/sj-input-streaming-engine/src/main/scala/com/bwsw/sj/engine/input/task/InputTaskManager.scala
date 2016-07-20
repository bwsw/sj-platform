package com.bwsw.sj.engine.input.task

import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, InputEnvironmentManager}
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.engine.core.managment.TaskManager
import com.hazelcast.config.{EvictionPolicy, MaxSizeConfig, XmlConfigBuilder}
import com.hazelcast.core.Hazelcast

/**
 * Class allowing to manage an environment of input streaming task
 * Created: 08/07/2016
 *
 * @author Kseniya Mikhaleva
 */
class InputTaskManager() extends TaskManager {

  val entryHost = System.getenv("ENTRY_HOST")
  val entryPort = System.getenv("ENTRY_PORT").toInt
  val inputInstance = instance.asInstanceOf[InputInstance]

  private val hazelcastMapName = "inputEngine"
  private val config = createHazelcastConfig()
  private val hazelcastInstance = Hazelcast.newHazelcastInstance(config)

  assert(agentsPorts.length >=
    (instance.outputs.length + 1),
    "Not enough ports for t-stream consumers/producers ")

  addEntryPointMetadataInInstance()

  /**
   * Returns a keys storage (Hazelcast map) for checking of there are duplicates (input envelopes) or not
    *
    * @return Storage of keys (Hazelcast map)
   */
  def getUniqueEnvelopes = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get hazelcast map for checking of there are duplicates (input envelopes) or not\n")
    hazelcastInstance.getMap[String, Array[Byte]](hazelcastMapName)
  }

  /**
   * Fills a task field in input instance with a current task name and entry host + port
   */
  private def addEntryPointMetadataInInstance() = {
    inputInstance.tasks.put(taskName, new InputTask(entryHost, entryPort))
    ConnectionRepository.getInstanceService.save(instance)
  }

  /**
   * Creates a Hazelcast map configuration
    *
    * @return Hazelcast map configuration
   */
  private def createHazelcastConfig() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create a Hazelcast map configuration is named '$hazelcastMapName'\n")
    val config = new XmlConfigBuilder().build()
    val evictionPolicy = createEvictionPolicy()
    val maxSizeConfig = createMaxSizeConfig()

    config.getMapConfig(hazelcastMapName)
      .setTimeToLiveSeconds(inputInstance.lookupHistory)
      .setEvictionPolicy(evictionPolicy)
      .setMaxSizeConfig(maxSizeConfig)

    config
  }

  /**
   * Creates an eviction policy for Hazelcast map configuration
    *
    * @return Eviction policy
   */
  private def createEvictionPolicy() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create EvictionPolicy\n")
    inputInstance.defaultEvictionPolicy match {
      case "LRU" => EvictionPolicy.LRU
      case "LFU" => EvictionPolicy.LFU
      case _ => EvictionPolicy.NONE
    }
  }

  /**
   * Creates a config that defines a max size of Hazelcast map
    *
    * @return Max size configuration
   */
  private def createMaxSizeConfig() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Create MaxSizeConfig\n")
    new MaxSizeConfig()
      .setSize(inputInstance.queueMaxSize)
  }

  /**
   * Returns an instance of executor of module
   *
   * @return An instance of executor of module
   */
  override def getExecutor(environmentManager: EnvironmentManager) = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    val moduleJar = getModuleJar
    val classLoader = getClassLoader(moduleJar.getAbsolutePath)
    val executor = classLoader.loadClass(fileMetadata.specification.executorClass)
      .getConstructor(classOf[InputEnvironmentManager])
      .newInstance(environmentManager).asInstanceOf[InputStreamingExecutor]
    logger.debug(s"Task: $taskName. Create instance of executor class\n")

    executor
  }

  /**
    * Returns an instance of executor of module
    *
    * @return An instance of executor of module
    */
  def getExecutor: StreamingExecutor = ???
}