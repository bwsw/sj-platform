package com.bwsw.sj.engine.input.task

import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, InputEnvironmentManager}
import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.input.config.InputEngineConfigNames
import com.bwsw.tstreams.agents.producer.Producer
import com.typesafe.config.ConfigFactory
import scaldi.Injector

import scala.util.Try

/**
  * Class allows to manage an environment of input streaming task
  *
  * @author Kseniya Mikhaleva
  */
class InputTaskManager(implicit injector: Injector) extends TaskManager {

  lazy val inputs: Nothing = {
    logger.error(s"Instance of Input module hasn't got execution plan " +
      s"and it's impossible to retrieve inputs.")
    throw new Exception(s"Instance of Input module hasn't got execution plan " +
      s"and it's impossible to retrieve inputs.")
  }

  val inputInstance = instance.asInstanceOf[InputInstance]
  val entryPort: Int = getEntryPort()
  val outputProducers: Map[String, Producer] = createOutputProducers()

  require(numberOfAgentsPorts >=
    (instance.outputs.length + 1),
    "Not enough ports for t-stream consumers/producers")

  def getEntryPort(): Int = {
    val config = ConfigFactory.load()
    Try(config.getInt(InputEngineConfigNames.entryPort))
      .getOrElse(inputInstance.tasks(taskName).port)
  }

  def getExecutor(environmentManager: EnvironmentManager): InputStreamingExecutor[AnyRef] = {
    logger.debug(s"Task: $taskName. Start loading an executor class from module jar.")
    val executor = executorClass.getConstructor(classOf[InputEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[InputStreamingExecutor[AnyRef]]
    logger.debug(s"Task: $taskName. Load an executor class.")

    executor
  }
}