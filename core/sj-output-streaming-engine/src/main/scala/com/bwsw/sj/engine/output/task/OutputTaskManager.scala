package com.bwsw.sj.engine.output.task


import com.bwsw.sj.common.dal.model.module.OutputInstance
import com.bwsw.sj.common.dal.model.stream.SjStream
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, OutputEnvironmentManager}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor

import scala.collection.mutable

/**
  * Task manager for working with streams of output-streaming module
  *
  * @author Kseniya Tomskikh
  */
class OutputTaskManager() extends TaskManager {
  val outputInstance: OutputInstance = instance.asInstanceOf[OutputInstance]
  val inputs: mutable.Map[SjStream, Array[Int]] = getInputs(outputInstance.executionPlan)

  require(numberOfAgentsPorts >= 2, "Not enough ports for t-stream consumers/producers ")

  def getExecutor(environmentManager: EnvironmentManager): OutputStreamingExecutor[AnyRef] = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar.")
    val executor = executorClass.getConstructor(classOf[OutputEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[OutputStreamingExecutor[AnyRef]]
    logger.debug(s"Task: $taskName. Create an instance of executor class.")

    executor
  }
}
