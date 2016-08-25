package com.bwsw.sj.engine.output.task

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.OutputEntity
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.sj.engine.core.managment.TaskManager

/**
 * Task manager for working with streams of output-streaming module
 * Created: 27/05/2016
 *
 * @author Kseniya Tomskikh
 */
class OutputTaskManager() extends TaskManager {

  /**
   * Returns an instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor = getExecutor()

  /**
   * Returns an instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(): StreamingExecutor = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    logger.debug(s"Task: $taskName. Create instance of executor class\n")
    val executor = moduleClassLoader
      .loadClass(executorClassName)
      .newInstance()
      .asInstanceOf[StreamingExecutor]
    logger.debug(s"Task: $taskName. Create instance of executor class\n")

    executor
  } //todo maybe needless

  /**
   * Getting instance of entity object from output module jar
   */
  def getOutputModuleEntity(): OutputEntity = {
    logger.info(s"Task: $taskName. Getting entity object from jar of file: " +
      instance.moduleType + "-" + instance.moduleName + "-" + instance.moduleVersion)
    val entityClassName = fileMetadata.specification.entityClass
    val outputEntity = moduleClassLoader
      .loadClass(entityClassName)
      .newInstance()
      .asInstanceOf[OutputEntity]

    outputEntity
  }
}
