package com.bwsw.sj.mesos.framework.task

import com.bwsw.sj.common.dal.ConnectionConstants
import com.bwsw.sj.common.dal.model.module._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.FrameworkRestEntity
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.{FrameworkUtil, OffersHandler}
import org.apache.log4j.Logger
import org.apache.mesos.Protos.{TaskID, TaskInfo, _}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object TasksList {
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
    tasksToLaunch += taskId
  }

  def toLaunch: mutable.ListBuffer[String] = {
    tasksToLaunch
  }

  def launched(taskId: String): ListBuffer[String] = {
    tasksToLaunch -= taskId
    launchedTasks += taskId
  }

  def stopped(taskId: String): ListBuffer[String] = {
    launchedTasks -= taskId
  }

  def killTask(taskId: String): ListBuffer[String] = {
    FrameworkUtil.driver.killTask(TaskID.newBuilder().setValue(taskId).build)
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

  def prepare(instance: Instance): Unit = {
    perTaskCores = FrameworkUtil.instance.perTaskCores
    perTaskMem = FrameworkUtil.instance.perTaskRam
    perTaskPortsCount = FrameworkUtil.getCountPorts(FrameworkUtil.instance)

    val tasks = FrameworkUtil.instance.moduleType match {
      case EngineLiterals.inputStreamingType =>
        (0 until FrameworkUtil.instance.parallelism).map(tn => FrameworkUtil.instance.name + "-task" + tn)
      case _ =>
        val executionPlan = FrameworkUtil.instance match {
          case regularInstance: RegularInstance => regularInstance.executionPlan
          case outputInstance: OutputInstance => outputInstance.executionPlan
          case batchInstance: BatchInstance => batchInstance.executionPlan
        }
        executionPlan.tasks.asScala.keys
    }
    tasks.foreach(task => newTask(task))
  }

  def createTaskToLaunch(task: String, offer: Offer): TaskInfo = {
    TaskInfo.newBuilder
      .setCommand(getCommand(task, offer))
      .setName(task)
      .setTaskId(TaskID.newBuilder.setValue(task).build())
      .addResources(OffersHandler.getCpusResource)
      .addResources(OffersHandler.getMemResource)
      .addResources(OffersHandler.getPortsResource(offer, task))
      .setSlaveId(offer.getSlaveId)
      .build()
  }

  def getCommand(task: String, offer: Offer): CommandInfo = {
    def getAgentPorts: String = {
      var agentPorts: String = ""
      var taskPort: String = ""
      val ports = OffersHandler.getPortsResource(offer, task)

      var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)

      val host = OffersHandler.getOfferIp(offer)
      TasksList(task).foreach(task => task.update(host=host))
      if (FrameworkUtil.instance.moduleType.equals(EngineLiterals.inputStreamingType)) {
        taskPort = availablePorts.head
        availablePorts = availablePorts.tail
        val inputInstance = FrameworkUtil.instance.asInstanceOf[InputInstance]
        inputInstance.tasks.put(task, new InputTask(host, taskPort.toInt))
        ConnectionRepository.getInstanceService.save(FrameworkUtil.instance)
      }

      agentPorts = availablePorts.mkString(",")
      agentPorts.dropRight(1)
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
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(FrameworkUtil.params {"instanceId"}))
        .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(task))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(OffersHandler.getOfferIp(offer)))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(getAgentPorts))
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(getInstanceHosts))
        .build()
    }

    CommandInfo.newBuilder
      .addUris(CommandInfo.URI.newBuilder.setValue(FrameworkUtil.getModuleUrl(FrameworkUtil.instance)))
      .setValue("java " + FrameworkUtil.getJvmOptions + " -jar " + FrameworkUtil.jarName)
      .setEnvironment(getEnvironments).build()
  }


  def addTaskToSlave(task: TaskInfo, offer: (Offer, Int)) = {
    if (launchedOffers.contains(offer._1.getId)) {
      launchedOffers(offer._1.getId) += task
    } else {
      launchedOffers += offer._1.getId -> ArrayBuffer(task)
    }
  }

}
