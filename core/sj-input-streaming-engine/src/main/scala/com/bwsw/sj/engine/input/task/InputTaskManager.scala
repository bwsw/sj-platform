package com.bwsw.sj.engine.input.task

import com.bwsw.sj.common.DAL.model.module.InputInstance
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

  val inputInstance = instance.asInstanceOf[InputInstance]
  val entryPort = inputInstance.tasks.get(taskName).port

  assert(agentsPorts.length >=
    (instance.outputs.length + 1),
    "Not enough ports for t-stream consumers/producers ")

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