package com.bwsw.sj.engine.input.task

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, InputEnvironmentManager}
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.engine.core.managment.TaskManager

/**
 * Class allowing to manage an environment of input streaming task
 *
 *
 * @author Kseniya Mikhaleva
 */
class InputTaskManager() extends TaskManager {

  lazy val inputs = {
    logger.error(s"Instance of Input module hasn't got execution plan " +
      s"and it's impossible to retrieve inputs.")
    throw new Exception(s"Instance of Input module hasn't got execution plan " +
      s"and it's impossible to retrieve inputs.")
  }

  val inputInstance = instance.asInstanceOf[InputInstance]
  val entryPort = getEntryPort()
  val outputProducers =  createOutputProducers()

  require(numberOfAgentsPorts >=
    (instance.outputs.length + 1),
    "Not enough ports for t-stream consumers/producers")

  def getEntryPort() = {
    if (System.getenv().containsKey("ENTRY_PORT"))
      System.getenv("ENTRY_PORT").toInt
    else inputInstance.tasks.get(taskName).port
  }

  override def getExecutor(environmentManager: EnvironmentManager) = {
    logger.debug(s"Task: $taskName. Start loading an executor class from module jar.")
    val executor = moduleClassLoader
      .loadClass(executorClassName)
      .getConstructor(classOf[InputEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[InputStreamingExecutor]
    logger.debug(s"Task: $taskName. Load an executor class.")

    executor
  }
}