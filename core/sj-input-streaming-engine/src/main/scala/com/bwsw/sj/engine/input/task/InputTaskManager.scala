package com.bwsw.sj.engine.input.task

import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, InputEnvironmentManager}
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.engine.core.managment.TaskManager

/**
 * Class allowing to manage an environment of input streaming task
 * Created: 08/07/2016
 *
 * @author Kseniya Mikhaleva
 */
class InputTaskManager() extends TaskManager {

  val entryPort = System.getenv("ENTRY_PORT").toInt
  val inputInstance = instance.asInstanceOf[InputInstance]

  assert(agentsPorts.length >=
    (instance.outputs.length + 1),
    "Not enough ports for t-stream consumers/producers ")

  addEntryPointMetadataInInstance()

  /**
   * Fills a task field in input instance with a current task name and entry host + port
   */
  private def addEntryPointMetadataInInstance() = {
    inputInstance.tasks.put(taskName, new InputTask(agentsHost, entryPort))
    ConnectionRepository.getInstanceService.save(instance)
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