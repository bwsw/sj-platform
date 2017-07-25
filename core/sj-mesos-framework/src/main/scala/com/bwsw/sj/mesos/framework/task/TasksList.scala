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
package com.bwsw.sj.mesos.framework.task

import com.bwsw.sj.common.dal.model.instance.InputTask
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.FrameworkRestEntity
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.{FrameworkParameters, FrameworkUtil, OffersHandler}
import org.apache.log4j.Logger
import org.apache.mesos.Protos.{TaskID, TaskInfo, _}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.{Failure, Success, Try}

object TasksList {
  /***
    * availablePorts - ports that can be used for current task. This parameter update after new offer come.
    */

  private val logger = Logger.getLogger(this.getClass)
  private val tasksToLaunch = mutable.ListBuffer[String]()
  private val listTasks = mutable.Map[String, Task]()
  private var message: String = "Initialization"
  private val availablePorts = collection.mutable.ListBuffer[Long]()
  private var launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()

  private var launchedTasks = mutable.ListBuffer[String]()

  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0

  def newTask(taskId: String) = {
    val task = new Task(taskId)
    listTasks += taskId -> task
//    tasksToLaunch += taskId
  }

  def getList: Iterable[Task] = {
    listTasks.values
  }

  def getTask(taskId: String): Task = {
    listTasks(taskId)
  }

  def addToLaunch(taskId: String) = {
    if (!tasksToLaunch.contains(taskId))
      tasksToLaunch += taskId
  }

  def toLaunch: mutable.ListBuffer[String] = {
    tasksToLaunch
  }

  def launched(taskId: String): ListBuffer[String] = {
    if (tasksToLaunch.contains(taskId))
      tasksToLaunch -= taskId
    if (!launchedTasks.contains(taskId))
      launchedTasks += taskId
    launchedTasks
  }

  def stopped(taskId: String): ListBuffer[String] = {
    if (launchedTasks.contains(taskId))
      launchedTasks -= taskId
    launchedTasks
  }

  def killTask(taskId: String): ListBuffer[String] = {
    FrameworkUtil.driver.get.killTask(TaskID.newBuilder().setValue(taskId).build)
    stopped(taskId)
  }

  def clearLaunchedTasks(): Unit = {
    launchedTasks = mutable.ListBuffer[String]()
  }

  def toFrameworkTask: FrameworkRestEntity = {
    FrameworkRestEntity(listTasks.values.map(_.toFrameworkTask).toSeq)
  }

  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }

  def clearAvailablePorts(): Unit = {
    availablePorts.remove(0, availablePorts.length)
  }

  def getAvailablePorts: ListBuffer[Long] = {
    availablePorts
  }

  def addAvailablePorts(ports: ListBuffer[Long]) = {
    availablePorts ++= ports
  }

  def count: Int = {
    toLaunch.size
  }

  def getLaunchedOffers: Map[OfferID, ArrayBuffer[TaskInfo]] = {
    launchedOffers
  }

  def getLaunchedTasks: ListBuffer[String] = {
    launchedTasks
  }

  def clearLaunchedOffers(): Unit = {
    launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()
  }

  def setMessage(message: String): Unit = {
    this.message = message
  }

  def getTaskPorts(task: String): Resource = {
    this.getTask(task).ports
  }

  def prepare(instance: Instance): Unit = {
    perTaskCores = FrameworkUtil.instance.get.perTaskCores
    perTaskMem = FrameworkUtil.instance.get.perTaskRam
    perTaskPortsCount = FrameworkUtil.getCountPorts(FrameworkUtil.instance.get)

    val tasks = FrameworkUtil.instance.get.moduleType match {
      case EngineLiterals.inputStreamingType =>
        (0 until FrameworkUtil.instance.get.countParallelism).map(tn => FrameworkUtil.instance.get.name + "-task" + tn)
      case _ =>
        val executionPlan = FrameworkUtil.instance.get match {
          case regularInstance: RegularInstance => regularInstance.executionPlan
          case outputInstance: OutputInstance => outputInstance.executionPlan
          case batchInstance: BatchInstance => batchInstance.executionPlan
        }
        executionPlan.tasks.asScala.keys
    }
    tasks.foreach(task => newTask(task))
  }

  def createTaskToLaunch(task: String, offer: Offer)(implicit injector: Injector): TaskInfo = {
    logger.debug("Creating task info")
    val taskInfo = TaskInfo.newBuilder
      .setCommand(getCommand(task, offer))
      .setName(task)
      .setTaskId(TaskID.newBuilder.setValue(task).build())
      .addResources(OffersHandler.getCpusResource)
      .addResources(OffersHandler.getMemResource)
      .addResources(TasksList.getTaskPorts(task))
      .setSlaveId(offer.getSlaveId)
      .build()
    taskInfo
  }

  def getCommand(task: String, offer: Offer)(implicit injector: Injector): CommandInfo = {
    TasksList(task).foreach(task => task.update(ports=OffersHandler.getPortsResource(offer)))
    def getAgentPorts: String = {
      var taskPort: String = ""
      val ports = TasksList.getTaskPorts(task)

      var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)

      val host = OffersHandler.getOfferIp(offer)
      TasksList(task).foreach(task => task.update(host=host))
      if (FrameworkUtil.instance.get.moduleType.equals(EngineLiterals.inputStreamingType)) {
        taskPort = availablePorts.head
        availablePorts = availablePorts.tail
        val inputInstance = FrameworkUtil.instance.get.asInstanceOf[InputInstance]
        inputInstance.tasks.put(task, new InputTask(host, taskPort.toInt))
        inject[ConnectionRepository].getInstanceRepository.save(FrameworkUtil.instance.get.to)
      }

      availablePorts.mkString(",")
    }

    def getInstanceHosts: String = {
      val hosts: collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer()
      TasksList.toLaunch.foreach(task =>
        if (TasksList.getTask(task).host.nonEmpty) hosts.append(TasksList.getTask(task).host.get)
      )
      hosts.mkString(",")
    }

    def getEnvironments: Environment = {
     Environment.newBuilder
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(FrameworkParameters(FrameworkParameters.instanceId)))
        .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(task))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(OffersHandler.getOfferIp(offer)))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(getAgentPorts))
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(getInstanceHosts))
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_HOSTS").setValue(FrameworkParameters(FrameworkParameters.mongoHosts)))
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_USER").setValue(FrameworkParameters(FrameworkParameters.mognoUser)))
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_PASSWORD").setValue(FrameworkParameters(FrameworkParameters.mongoPassword)))
//        .addVariables(Environment.Variable.newBuilder.setName("ENTRY_PORT").setValue("8888"))
        .build()
    }

    FrameworkUtil.getModuleUrl(FrameworkUtil.instance.get)
    logger.info("java " + FrameworkUtil.getJvmOptions + " -jar " + FrameworkUtil.jarName.get)

    val cmdInfo = CommandInfo.newBuilder
      .addUris(CommandInfo.URI.newBuilder.setValue(FrameworkUtil.getModuleUrl(FrameworkUtil.instance.get)))
      .setValue("java " + FrameworkUtil.getJvmOptions + " -jar " + FrameworkUtil.jarName.get)
      .setEnvironment(getEnvironments).build()


    logger.debug("Complete building command info")

    cmdInfo
  }


  def addTaskToSlave(task: TaskInfo, offer: (Offer, Int)) = {
    if (launchedOffers.contains(offer._1.getId)) {
      launchedOffers(offer._1.getId) += task
    } else {
      launchedOffers += offer._1.getId -> ArrayBuffer(task)
    }
  }

}
