package com.bwsw.sj.mesos.framework.schedule

import java.net.URI
import java.util

import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.task.{StatusHandler, TasksList}
import org.apache.log4j.Logger
import org.apache.mesos.Protos._
import org.apache.mesos.{Scheduler, SchedulerDriver}
import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.collection.mutable


/**
  * Mesos scheduler implementation
  */
class FrameworkScheduler extends Scheduler {

  private val logger = Logger.getLogger(this.getClass)
  var params = immutable.Map[String, String]()
  val configFileService = ConnectionRepository.getConfigService
  var jarName: String = null
  var uniqueHosts = false


  def error(driver: SchedulerDriver, message: String) {
    logger.error(s"Got error message: $message")
    TasksList.message = s"Got error message: $message"
  }

  def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int): Unit = {
    /// TODO:
  }

  def slaveLost(driver: SchedulerDriver, slaveId: SlaveID): Unit = {
    /// TODO:
  }

  def disconnected(driver: SchedulerDriver): Unit = {
    /// TODO:
  }

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]) {
    logger.debug(s"Got framework message: $data")
    TasksList.message = s"Got framework message: $data"
  }


  /**
   * Execute when task change status.
   * @param driver scheduler driver
   * @param status received status from master
   */
  def statusUpdate(driver: SchedulerDriver, status: TaskStatus) {
    StatusHandler.handle(status)
  }

  def getOfferIp(offer:Offer) = {
    offer.getUrl.getAddress.getIp
  }

  def offerRescinded(driver: SchedulerDriver, offerId: OfferID): Unit = {
    /// TODO:
  }


  /**
   * Obtaining resources and launch tasks.
   *
   * @param driver scheduler driver
   * @param offers resources, that master offered to framework
   */
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    logger.info(s"RESOURCE OFFERS")
    TasksList.clearAvailablePorts()
    OfferHandler.filter(offers, FrameworkUtil.instance.nodeAttributes)
    if (OfferHandler.filteredOffers.size == 0) {
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      TasksList.message = "No one node selected"
      logger.info("No one node selected")
      return
    }

    for (offer <- OfferHandler.filteredOffers.asScala) {
      logger.debug(s"Offer ID: ${offer.getId.getValue}")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}")
    }

    var tasksCountOnSlaves = OfferHandler.getOffersForSlave(TasksList.perTaskCores,
      TasksList.perTaskMem,
      TasksList.perTaskPortsCount,
      TasksList.count,
      OfferHandler.filteredOffers)

    var overTasks = 0
    for (slave <- tasksCountOnSlaves) {
      overTasks += slave._2
    }


    if (uniqueHosts && tasksCountOnSlaves.length < overTasks) {
      overTasks = tasksCountOnSlaves.length
    }


    logger.debug(s"Count tasks can be launched: $overTasks")
    logger.debug(s"Count tasks must be launched: ${TasksList.count}")

    if (TasksList.count > overTasks) {
      logger.info(s"Can not launch tasks: no required resources")
      TasksList.message = "Can not launch tasks: no required resources"
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      return
    }

    logger.debug(s"Tasks to launch: ${TasksList.toLaunch}")

    var offerNumber = 0
    var launchedTasks: mutable.Map[OfferID, mutable.ListBuffer[TaskInfo]] = mutable.Map()
    for (currTask <- TasksList.toLaunch) {

      val currentOffer = tasksCountOnSlaves(offerNumber)
      if (offerNumber >= tasksCountOnSlaves.size - 1) {
        offerNumber = 0
      } else {
        offerNumber += 1
      }

      if (offers.asScala.contains(currentOffer._1)) {
        offers.asScala.remove(offers.asScala.indexOf(currentOffer._1))
      }


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
      val ports = getPorts(currentOffer._1, currTask)

      var agentPorts: String = ""
      var taskPort: String = ""

      var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)

      if (FrameworkUtil.instance.moduleType.equals(EngineLiterals.inputStreamingType)) {
        taskPort = availablePorts.head
        availablePorts = availablePorts.tail
        val inputInstance = FrameworkUtil.instance.asInstanceOf[InputInstance]
        val host = getOfferIp(currentOffer._1)
        TasksList(currTask).foreach(task => task.update(host=host))
        inputInstance.tasks.put(currTask, new InputTask(host, taskPort.toInt))
        ConnectionRepository.getInstanceService.save(FrameworkUtil.instance)
      }
      agentPorts = availablePorts.mkString(",")
      agentPorts.dropRight(1)

      logger.debug(s"Task: $currTask. Ports for task: $ports")
      logger.debug(s"Task: $currTask. Agent ports: $agentPorts")

      val cmd = CommandInfo.newBuilder()
      val hosts: collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer()
      TasksList.toLaunch.foreach(task =>
        if (TasksList.getTask(task).host.nonEmpty) hosts.append(TasksList.getTask(task).host.get)
      )
      try {
        val environments = Environment.newBuilder
          .addVariables(Environment.Variable.newBuilder.setName("MONGO_HOST").setValue(params {"mongodbHost"}))
          .addVariables(Environment.Variable.newBuilder.setName("MONGO_PORT").setValue(params {"mongodbPort"}))
          .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(params {"instanceId"}))
          .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(currTask))
          .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(getOfferIp(currentOffer._1)))
          .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(agentPorts))
          .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(hosts.mkString(",")))

        cmd
          .addUris(CommandInfo.URI.newBuilder.setValue(getModuleUrl(FrameworkUtil.instance)))
          .setValue("java -jar " + jarName)
          .setEnvironment(environments)
      } catch {
        case e: Exception => FrameworkUtil.handleSchedulerException(e, logger)
      }

      while (tasksCountOnSlaves(offerNumber)._2 == 0) {
        tasksCountOnSlaves = tasksCountOnSlaves.filterNot(_ == tasksCountOnSlaves(offerNumber))
        if (offerNumber > tasksCountOnSlaves.size - 1) {
          offerNumber = 0
        }
      }

      logger.info(s"Task: $currTask => Slave: ${currentOffer._1.getSlaveId.getValue}")

      val task = TaskInfo.newBuilder
        .setCommand(cmd)
        .setName(currTask)
        .setTaskId(TaskID.newBuilder.setValue(currTask))
        .addResources(cpus)
        .addResources(mem)
        .addResources(ports)
        .setSlaveId(currentOffer._1.getSlaveId)
        .build()

      if (launchedTasks.contains(currentOffer._1.getId)) {
        launchedTasks(currentOffer._1.getId) += task
      } else {
        launchedTasks += (currentOffer._1.getId -> mutable.ListBuffer(task))
      }

      // update how much tasks we can run on slave when launch current task
      tasksCountOnSlaves.update(tasksCountOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
      TasksList.launched(currTask)
    }

    for (task <- launchedTasks) {
      driver.launchTasks(List(task._1).asJava, task._2.asJava)
    }

    for (offer <- offers.asScala) {
      driver.declineOffer(offer.getId)
    }
    TasksList.message = "Tasks launched"
  }


  /**
   * Reregistering framework after master disconnected.
   */
  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {
    logger.debug(s"New master $masterInfo")
    TasksList.message = s"New master $masterInfo"
  }


  /**
   * Registering framework.
   */
  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework as: ${frameworkId.getValue}")

    FrameworkUtil.driver = driver
    FrameworkUtil.frameworkId = frameworkId.getValue
    FrameworkUtil.master = masterInfo

    params = FrameworkUtil.getEnvParams()
    logger.debug(s"Got environment variable: $params")

    val optionInstance = ConnectionRepository.getInstanceService.get(params("instanceId"))

    if (optionInstance.isEmpty) {
      logger.error(s"Not found instance")
      TasksList.message = "Framework shut down: not found instance."
      driver.stop()
      return
    } else { FrameworkUtil.instance = optionInstance.get}
    logger.debug(s"Got instance ${FrameworkUtil.instance.name}")

    TasksList.prepare(FrameworkUtil.instance)
    logger.debug(s"Got tasks: $TasksList")

    scala.util.Try(System.getenv("UNIQUE_HOSTS").toBoolean).getOrElse(false)

    TasksList.message = s"Registered framework as: ${frameworkId.getValue}"
  }


  /**
   * Get jar URI for framework
   * @param instance:Instance
   * @return String
   */
  def getModuleUrl(instance: Instance): String = {
    jarName = configFileService.get("system." + instance.engine).get.value
    val restHost = configFileService.get(ConfigLiterals.hostOfCrudRestTag).get.value
    val restPort = configFileService.get(ConfigLiterals.portOfCrudRestTag).get.value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/jars/$jarName").toString
    logger.debug(s"Engine downloading URL: $restAddress")
    restAddress
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
}

