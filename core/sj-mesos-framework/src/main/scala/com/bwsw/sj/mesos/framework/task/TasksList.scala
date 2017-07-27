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


/**
  * Data model for list of tasks
  */
object TasksList {
  private val logger = Logger.getLogger(this.getClass)
  private val tasksToLaunch = mutable.ListBuffer[String]()
  private val listTasks = mutable.Map[String, Task]()
  private var message: String = "Initialization"
  private var launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()
  private var launchedTasks = mutable.ListBuffer[String]()

  /** availablePorts - ports that can be used for current task. This parameter is updated after new offer come. */
  private val availablePorts = collection.mutable.ListBuffer[Long]()

  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0

  /**
    * Creates new task
    * @param taskId
    * @return
    */
  def newTask(taskId: String) = {
    val task = new Task(taskId)
    listTasks += taskId -> task
//    tasksToLaunch += taskId
  }

  /**
    * Returns all tasks
    * @return
    */
  def getList: Iterable[Task] = {
    listTasks.values
  }

  /**
    * Returns task by ID
    * @param taskId
    * @return
    */
  def getTask(taskId: String): Task = {
    listTasks(taskId)
  }

  /**
    * Adds tasks to launch
    * @param taskId
    * @return
    */
  def addToLaunch(taskId: String) = {
    if (!tasksToLaunch.contains(taskId))
      tasksToLaunch += taskId
  }

  /**
    * Returns tasks ready to launch
    * @return mutable.ListBuffer[String]
    */
  def toLaunch: mutable.ListBuffer[String] = {
    tasksToLaunch
  }

  /**
    * Adds task to launched list
    * @param taskId
    * @return ListBuffer[String]
    */
  def launched(taskId: String): ListBuffer[String] = {
    if (tasksToLaunch.contains(taskId))
      tasksToLaunch -= taskId
    if (!launchedTasks.contains(taskId))
      launchedTasks += taskId
    launchedTasks
  }

  /**
    * Removes task from launched tasks list
    * @param taskId
    * @return ListBuffer[String]
    */
  def stopped(taskId: String): ListBuffer[String] = {
    if (launchedTasks.contains(taskId))
      launchedTasks -= taskId
    launchedTasks
  }

  /**
    * Kills task by ID
    * @param taskId
    * @return ListBuffer[String]
    */
  def killTask(taskId: String): ListBuffer[String] = {
    FrameworkUtil.driver.get.killTask(TaskID.newBuilder().setValue(taskId).build)
    stopped(taskId)
  }

  /**
    * Clears launched tasks list
    */
  def clearLaunchedTasks(): Unit = {
    launchedTasks = mutable.ListBuffer[String]()
  }

  /**
    * Transform to FrameworkTask
    * @return FrameworkRestEntity
    */
  def toFrameworkTask: FrameworkRestEntity = {
    FrameworkRestEntity(listTasks.values.map(_.toFrameworkTask).toSeq)
  }

  /**
    * Returns task from tasks list by ID
    * @param taskId Task id
    * @return Option[Task]
    */
  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }

  /**
    * Clears available list of ports
    */
  def clearAvailablePorts(): Unit = {
    availablePorts.remove(0, availablePorts.length)
  }

  /**
    * Returns available ports
    * @return ListBuffer[Long]
    */
  def getAvailablePorts: ListBuffer[Long] = {
    availablePorts
  }

  /**
    * Adds to available ports list new list of ports
    * @param ports List of ports
    * @return Unit
    */
  def addAvailablePorts(ports: ListBuffer[Long]) = {
    availablePorts ++= ports
  }

  /**
    * Returns how much tasks to launch
    * @return
    */
  def count: Int = {
    toLaunch.size
  }

  /**
    * Returns offers list where tasks launched
    * @return
    */
  def getLaunchedOffers: Map[OfferID, ArrayBuffer[TaskInfo]] = {
    launchedOffers
  }

  /**
    * Returns launched tasks list
    * @return
    */
  def getLaunchedTasks: ListBuffer[String] = {
    launchedTasks
  }

  /**
    * Clear offers list with tasks
    */
  def clearLaunchedOffers(): Unit = {
    launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()
  }

  /**
    * Sets message to display
    * @param message String
    */
  def setMessage(message: String): Unit = {
    this.message = message
  }

  /**
    * Returns ports resource occupied by task
    * @param taskId
    * @return
    */
  def getTaskPorts(taskId: String): Resource = {
    this.getTask(taskId).ports
  }

  /**
    * Initializes tasks list, fetch tasks from instance
    * @param instance
    */
  def prepareTasks(instance: Instance): Unit = {
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

  /**
    * Prepares task to launch on offer
    * @param taskId Current task id
    * @param offer Current offer
    * @param injector
    * @return
    */
  def createTaskToLaunch(taskId: String, offer: Offer)(implicit injector: Injector): TaskInfo = {
    logger.debug("Creating task info")
    val taskInfo = TaskInfo.newBuilder
      .setCommand(getCommand(taskId, offer))
      .setName(taskId)
      .setTaskId(TaskID.newBuilder.setValue(taskId).build())
      .addResources(OffersHandler.getCpusResource)
      .addResources(OffersHandler.getMemResource)
      .addResources(TasksList.getTaskPorts(taskId))
      .setSlaveId(offer.getSlaveId)
      .build()
    taskInfo
  }

  /**
    * Prepares command for task
    * @param taskId Current task id
    * @param offer Current offer
    * @param injector
    * @return
    */
  def getCommand(taskId: String, offer: Offer)(implicit injector: Injector): CommandInfo = {
    TasksList(taskId).foreach(task => task.update(ports=OffersHandler.getPortsResource(offer)))
    /**
      * Return available ports for current offer
      * @return
      */
    def getAgentPorts: String = {
      var taskPort: String = ""
      val ports = TasksList.getTaskPorts(taskId)

      var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)

      val host = OffersHandler.getOfferIp(offer)
      TasksList(taskId).foreach(task => task.update(host=host))
      if (FrameworkUtil.instance.get.moduleType.equals(EngineLiterals.inputStreamingType)) {
        taskPort = availablePorts.head
        availablePorts = availablePorts.tail
        val inputInstance = FrameworkUtil.instance.get.asInstanceOf[InputInstance]
        inputInstance.tasks.put(taskId, new InputTask(host, taskPort.toInt))
        inject[ConnectionRepository].getInstanceRepository.save(FrameworkUtil.instance.get.to)
      }

      availablePorts.mkString(",")
    }

    /**
      * Returns hosts from all tasks
      * @return String
      */
    def getInstanceHosts: String = {
      val hosts: collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer()
      TasksList.toLaunch.foreach(task =>
        if (TasksList.getTask(task).host.nonEmpty) hosts.append(TasksList.getTask(task).host.get)
      )
      hosts.mkString(",")
    }

    /**
      * Prepares environment
      * @return Environment
      */
    def getEnvironment: Environment = {
     Environment.newBuilder
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(FrameworkParameters(FrameworkParameters.instanceId)))
        .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(taskId))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(OffersHandler.getOfferIp(offer)))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(getAgentPorts))
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(getInstanceHosts))
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_HOSTS").setValue(FrameworkParameters(FrameworkParameters.mongoHosts)))
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_USER").setValue(FrameworkParameters(FrameworkParameters.mongoUser)))
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_PASSWORD").setValue(FrameworkParameters(FrameworkParameters.mongoPassword)))
//        .addVariables(Environment.Variable.newBuilder.setName("ENTRY_PORT").setValue("8888"))
        .build()
    }

    FrameworkUtil.getModuleUrl(FrameworkUtil.instance.get)
    logger.info("java " + FrameworkUtil.getJvmOptions + " -jar " + FrameworkUtil.jarName.get)

    val cmdInfo = CommandInfo.newBuilder
      .addUris(CommandInfo.URI.newBuilder.setValue(FrameworkUtil.getModuleUrl(FrameworkUtil.instance.get)))
      .setValue("java " + FrameworkUtil.getJvmOptions + " -jar " + FrameworkUtil.jarName.get)
      .setEnvironment(getEnvironment).build()


    logger.debug("Complete building command info")

    cmdInfo
  }

  /**
    * Adds launched task to current offer
    * @param task Current task info
    * @param offer Current offer
    * @return
    */
  def addTaskToSlave(task: TaskInfo, offer: (Offer, Int)) = {
    if (launchedOffers.contains(offer._1.getId)) {
      launchedOffers(offer._1.getId) += task
    } else {
      launchedOffers += offer._1.getId -> ArrayBuffer(task)
    }
  }

}
