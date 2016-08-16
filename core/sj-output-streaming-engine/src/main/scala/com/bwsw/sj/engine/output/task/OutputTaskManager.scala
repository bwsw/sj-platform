package com.bwsw.sj.engine.output.task

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.OutputEntity
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.output.OutputStreamingHandler

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
 * Task manager for working with streams of output-streaming module
 * Created: 27/05/2016
 *
 * @author Kseniya Tomskikh
 */
class OutputTaskManager() extends TaskManager {

  val task: mutable.Map[String, Array[Int]] = instance.executionPlan.tasks.get(taskName).inputs.asScala

  /**
   * Returns an instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor = ???

  /**
   * Returns instance of executor of module
   *
   * @return An instance of executor of module
   */
  def getExecutor: StreamingExecutor = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar\n")
    val moduleJar = getModuleJar
    val classLoader = getClassLoader(moduleJar.getAbsolutePath)

    logger.debug(s"Task: $taskName. Create instance of executor class\n")
    val executor = classLoader
      .loadClass(fileMetadata.specification.executorClass)
      .newInstance()
      .asInstanceOf[OutputStreamingHandler]

    executor
  }

  /**
   * Getting instance of entity object from output module jar
   */
  def getOutputModuleEntity() = {
    val file = getModuleJar
    val entityClassName = fileMetadata.specification.entityClass
    logger.info(s"Task: $taskName. Getting entity object from jar of file: ${instance.moduleType}-${instance.moduleName}-${instance.moduleVersion}")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(entityClassName)
    clazz.newInstance().asInstanceOf[OutputEntity]
  }
}
