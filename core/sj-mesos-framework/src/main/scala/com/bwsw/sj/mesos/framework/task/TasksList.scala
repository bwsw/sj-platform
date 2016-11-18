package com.bwsw.sj.mesos.framework.task

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.{FrameworkUtil, OfferHandler}
import org.apache.log4j.Logger
import org.apache.mesos.Protos.{TaskID, TaskInfo, _}

import scala.collection.JavaConverters._
import scala.collection.mutable

object TasksList {
  private val logger = Logger.getLogger(this.getClass)
  private val tasksToLaunch: mutable.ListBuffer[String] = mutable.ListBuffer()
  private val listTasks : mutable.Map[String, Task] = mutable.Map()
  var message: String = "Initialization"
  var availablePorts: collection.mutable.ListBuffer[Long] = collection.mutable.ListBuffer()
  var launchedTasks: mutable.Map[OfferID, mutable.ListBuffer[TaskInfo]] = mutable.Map()

  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0

  def newTask(taskId: String) = {
    val task = new Task(taskId)
    listTasks += taskId -> task
    tasksToLaunch += taskId
  }

  def getList = {
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

  def launched(taskId: String) = {
    tasksToLaunch -= taskId
  }

  def toJson: Map[String, Any] = {
    Map(("tasks", listTasks.values.map(_.toJson)),
      ("message", message))
  }

  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }

  def clearAvailablePorts() = {
    availablePorts.remove(0, availablePorts.length)
  }

  def count = {
    toLaunch.size
  }

  def prepare(instance: Instance) = {
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
          case windowedInstance: WindowedInstance => windowedInstance.executionPlan
        }
        executionPlan.tasks.asScala.keys
    }
    tasks.foreach(task => newTask(task))
  }


  def createTaskToLaunch(task: String, offer: Offer): TaskInfo = {
    // Task Resources
    val cpus = Resource.newBuilder
      .setType(Value.Type.SCALAR)
      .setName("cpus")
      .setScalar(Value.Scalar.newBuilder.setValue(TasksList.perTaskCores))
      .build
    val mem = Resource.newBuilder
      .setType(org.apache.mesos.Protos.Value.Type.SCALAR)
      .setName("mem")
      .setScalar(org.apache.mesos.Protos.Value.
        Scalar.newBuilder.setValue(TasksList.perTaskMem)
      ).build
    val ports = getPorts(offer, task)

    var agentPorts: String = ""
    var taskPort: String = ""

    var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)

    val host = OfferHandler.getOfferIp(offer)
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

    logger.debug(s"Task: $task. Ports for task: $ports")
    logger.debug(s"Task: $task. Agent ports: $agentPorts")

    val cmd = CommandInfo.newBuilder()
    val hosts: collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer()
    TasksList.toLaunch.foreach(task =>
      if (TasksList.getTask(task).host.nonEmpty) hosts.append(TasksList.getTask(task).host.get)
    )
    try {
      val environments = Environment.newBuilder
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_HOST").setValue(FrameworkUtil.params {"mongodbHost"}))
        .addVariables(Environment.Variable.newBuilder.setName("MONGO_PORT").setValue(FrameworkUtil.params {"mongodbPort"}))
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(FrameworkUtil.params {"instanceId"}))
        .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(task))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(OfferHandler.getOfferIp(offer)))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(agentPorts))
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(hosts.mkString(",")))

      cmd
        .addUris(CommandInfo.URI.newBuilder.setValue(FrameworkUtil.getModuleUrl(FrameworkUtil.instance)))
        .setValue("java -jar " + FrameworkUtil.jarName)
        .setEnvironment(environments)
    } catch {
      case e: Exception => FrameworkUtil.handleSchedulerException(e, logger)
    }
    logger.info(s"Task: $task => Slave: ${offer.getSlaveId.getValue}")

    TaskInfo.newBuilder
      .setCommand(cmd)
      .setName(task)
      .setTaskId(TaskID.newBuilder.setValue(task))
      .addResources(cpus)
      .addResources(mem)
      .addResources(ports)
      .setSlaveId(offer.getSlaveId)
      .build()
  }


  /**
    * Get random unused ports
    * @param offer:Offer
    * @param task:String
    * @return Resource
    */
  def getPorts(offer: Offer, task: String): Resource = {
    val portsResource = OfferHandler.getResource(offer, "ports")
    for (range <- portsResource.getRanges.getRangeList.asScala) {
      TasksList.availablePorts ++= (range.getBegin to range.getEnd).to[mutable.ListBuffer]
    }

    val ports: mutable.ListBuffer[Long] = TasksList.availablePorts.take(TasksList.perTaskPortsCount)
    TasksList.availablePorts.remove(0, TasksList.perTaskPortsCount)

    val ranges = Value.Ranges.newBuilder
    for (port <- ports) {
      ranges.addRange(Value.Range.newBuilder.setBegin(port).setEnd(port))
    }

    Resource.newBuilder
      .setName("ports")
      .setType(Value.Type.RANGES)
      .setRanges(ranges)
      .build
  }


  def addTaskToSlave(task: TaskInfo, offer: (Offer, Int)) = {
    if (launchedTasks.contains(offer._1.getId)) {
      launchedTasks(offer._1.getId) += task
    } else {
      launchedTasks += (offer._1.getId -> mutable.ListBuffer(task))
    }
  }

}
