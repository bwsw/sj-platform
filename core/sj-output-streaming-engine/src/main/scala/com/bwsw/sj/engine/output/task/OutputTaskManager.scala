package com.bwsw.sj.engine.output.task

import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.OutputData
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.sj.engine.core.managment.TaskManager

/**
 * Task manager for working with streams of output-streaming module
 *
 * @author Kseniya Tomskikh
 */
class OutputTaskManager() extends TaskManager {

  private val outputInstance = instance.asInstanceOf[OutputInstance]
  val inputs = getInputs(outputInstance.executionPlan)
  lazy val outputProducers = {
    logger.error(s"Instance of Output module hasn't got t-stream outputs " +
      s"and it's impossible to retrieve their producers")
    throw new Exception(s"Instance of Output module hasn't got t-stream outputs " +
      s"and it's impossible to retrieve their producers")
  }
  
  assert(agentsPorts.length == 2, "Not enough ports for t-stream consumers/producers ")

  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    logger.debug(s"Task: $taskName. Create instance of executor class\n")
    val executor = moduleClassLoader
      .loadClass(executorClassName)
      .newInstance()
      .asInstanceOf[StreamingExecutor]
    logger.debug(s"Task: $taskName. Create instance of executor class\n")

    executor
  }

  def getOutputModuleEntity(): OutputData = {
    logger.info(s"Task: $taskName. Getting entity object from jar of file: " +
      instance.moduleType + "-" + instance.moduleName + "-" + instance.moduleVersion)
    val entityClassName = fileMetadata.specification.entityClass
    val outputEntity = moduleClassLoader
      .loadClass(entityClassName)
      .newInstance()
      .asInstanceOf[OutputData]

    outputEntity
  }
}
