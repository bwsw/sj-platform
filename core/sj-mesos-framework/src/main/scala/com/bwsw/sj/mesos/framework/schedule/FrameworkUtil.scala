/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}
import java.net.URI

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.utils.{CommonAppConfigNames, FrameworkLiterals}
import com.bwsw.sj.mesos.framework.task.TasksList
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.apache.mesos.Protos.MasterInfo
import org.apache.mesos.SchedulerDriver
import scaldi.Injectable.inject

import scala.collection.immutable
import scala.util.Properties
import scala.collection.JavaConverters._
import scala.util.Try


object FrameworkUtil {

  import com.bwsw.sj.common.SjModule._

  // Java threads created from JNI code in a non-java thread have null ContextClassloader unless the creator explicitly sets it.
  // Also in such context Thread.currentThread().getContextClassLoader() returns null.
  private val classLoader = ClassLoader.getSystemClassLoader
  Thread.currentThread().setContextClassLoader(classLoader)

  var master: Option[MasterInfo] = None
  var frameworkId: Option[String] = None
  var driver: Option[SchedulerDriver] = None
  var jarName: Option[String] = None
  var instance: Option[Instance] = None
  val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  val configRepository: GenericMongoRepository[ConfigurationSettingDomain] = connectionRepository.getConfigRepository
  private val logger = Logger.getLogger(this.getClass)
  var params: Map[String, String] = immutable.Map[String, String]()

  /**
    * Count how much ports must be for current task.
    *
    * @param instance current launched task
    * @return ports count for current task
    */
  def getCountPorts(instance: Instance): Int = {
    instance match {
      case _: OutputInstance => 2
      case regularInstance: RegularInstance => regularInstance.inputs.length + regularInstance.outputs.length + 4
      case inputInstance: InputInstance => inputInstance.outputs.length + 2
      case batchInstance: BatchInstance => batchInstance.inputs.length + batchInstance.outputs.length + 4
    }
  }

  /**
    * Handler for Scheduler Exception
    */
  def handleSchedulerException(e: Exception, logger: Logger): Unit = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.setMessage(e.getMessage)
    logger.error(s"Framework error: ${sw.toString}")
    driver.foreach(_.stop())
    System.exit(1)
  }

  def getEnvParams: Map[String, String] = {
    val config = ConfigFactory.load()

    Map(
      "instanceId" ->
        Try(config.getString(FrameworkLiterals.instanceId)).getOrElse("00000000-0000-0000-0000-000000000000"),
      "mongodbHosts" -> Try(config.getString(CommonAppConfigNames.mongoHosts)).getOrElse("127.0.0.1:27017")
    )
  }

  /**
    * Get jar URI for framework
    *
    * @param instance :Instance
    * @return String
    */
  def getModuleUrl(instance: Instance): String = {
    jarName = configRepository.get("system." + instance.engine).map(_.value)
    val restHost = configRepository.get(ConfigLiterals.hostOfCrudRestTag).get.value
    val restPort = configRepository.get(ConfigLiterals.portOfCrudRestTag).get.value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/jars/${jarName.get}").toString
    logger.debug(s"Engine downloading URL: $restAddress.")
    restAddress
  }

  def isInstanceStarted: Boolean = {
    updateInstance()
    instance.exists(_.status == "started")
  }

  def killAllLaunchedTasks(): Unit = {
    TasksList.getLaunchedTasks.foreach(taskId => {
      TasksList.killTask(taskId)
    })
  }

  /**
    * Teardown framework, do it if instance not started.
    */
  def teardown(): Unit = {
    logger.info(s"Kill all launched tasks: ${TasksList.getLaunchedTasks}")
    killAllLaunchedTasks()
  }

  /**
    * Selecting which tasks would be launched
    */
  def prepareTasksToLaunch(): Unit = {
    TasksList.getList.foreach(task => {
      if (!TasksList.getLaunchedTasks.contains(task.id)) TasksList.addToLaunch(task.id)
    })
    logger.info(s"Selecting tasks to launch: ${TasksList.toLaunch}")
  }


  def updateInstance(): Any = {
    val optionInstance = connectionRepository.getInstanceRepository.get(FrameworkUtil.params("instanceId"))
      .map(inject[InstanceCreator].from)

    if (optionInstance.isEmpty) {
      logger.error(s"Not found instance")
      TasksList.setMessage("Framework shut down: not found instance.")
      driver.foreach(_.stop())
    } else {
      FrameworkUtil.instance = optionInstance
    }
  }

  def getJvmOptions: String = {
    instance.get.jvmOptions.asScala.foldLeft("")((acc, option) => s"$acc ${option._1}${option._2}")
  }

  def checkInstanceStarted(): Unit = {
    logger.info(s"Check is instance status 'started': $isInstanceStarted")
    if (isInstanceStarted) prepareTasksToLaunch()
    else teardown()
  }
}
