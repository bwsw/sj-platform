package com.bwsw.sj.mesos.framework.schedule

import java.net.URI
import java.util

import com.bwsw.sj.common.DAL.model.module.{InputTask, Instance, InputInstance}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.{ConfigConstants, ModuleConstants}
import com.bwsw.sj.mesos.framework.task.{StatusHandler, TasksList}
import org.apache.log4j.Logger
import org.apache.mesos.Protos._
import org.apache.mesos.{Scheduler, SchedulerDriver}

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.collection.mutable
import scala.util.Properties


/*
 * Mesos scheduler implementation
 */
class FrameworkScheduler extends Scheduler {

  private val logger = Logger.getLogger(this.getClass)

  var driver: SchedulerDriver = null
  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0
  var params = immutable.Map[String, String]()
  var instance: Option[Instance] = None
  val configFileService = ConnectionRepository.getConfigService
  var jarName: String = null
  val availablePortsForOneInstance: collection.mutable.ListBuffer[Long] = collection.mutable.ListBuffer()
  var uniqueHosts = false

  def error(driver: SchedulerDriver, message: String) {
    logger.error(s"Got error message: $message")
    TasksList.message = s"Got error message: $message"
  }

  def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int) {}

  def slaveLost(driver: SchedulerDriver, slaveId: SlaveID) {}

  def disconnected(driver: SchedulerDriver) {}

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]) {
    logger.debug(s"Got framework message: $data")
    TasksList.message = s"Got framework message: $data"
  }

  def statusUpdate(driver: SchedulerDriver, status: TaskStatus) {
    StatusHandler.handle(status)
  }

  def offerRescinded(driver: SchedulerDriver, offerId: OfferID) {
  }


  /**
    * Obtaining slaves
    *
    * @param driver:SchedulerDriver
    * @param offers:util.List[Offer]
    */
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    logger.info(s"RESOURCE OFFERS")
    availablePortsForOneInstance.remove(0, availablePortsForOneInstance.length)

    val filteredOffers = filterOffers(offers, this.instance.get.nodeAttributes)
    if (filteredOffers.size == 0) {
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      TasksList.message = "No one node selected"
      logger.info("No one node selected")
      return
    }

    for (offer <- filteredOffers.asScala) {
      logger.debug(s"Offer ID: ${offer.getId.getValue}")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}")
    }

    val tasksCount = TasksList.toLaunch.size
    var tasksCountOnSlaves = getOffersForSlave(perTaskCores,
      perTaskMem,
      perTaskPortsCount,
      tasksCount,
      filteredOffers)

    var overTasks = 0
    for (slave <- tasksCountOnSlaves) {
      overTasks += slave._2
    }


    if (uniqueHosts && tasksCountOnSlaves.length < overTasks) {
      overTasks = tasksCountOnSlaves.length
    }


    logger.debug(s"Count tasks can be launched: $overTasks")
    logger.debug(s"Count tasks must be launched: $tasksCount")

    if (tasksCount > overTasks) {
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
        .setScalar(Value.Scalar.newBuilder.setValue(perTaskCores))
        .build
      val mem = Resource.newBuilder
        .setType(org.apache.mesos.Protos.Value.Type.SCALAR)
        .setName("mem")
        .setScalar(org.apache.mesos.Protos.Value.
          Scalar.newBuilder.setValue(perTaskMem)
        ).build
      val ports = getPorts(currentOffer._1, currTask)

      var agentPorts: String = ""
      var taskPort: String = ""

      var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)
      if (instance.get.moduleType.equals(ModuleConstants.inputStreamingType)) {
        taskPort = availablePorts.head; availablePorts = availablePorts.tail
        val inputInstance = instance.get.asInstanceOf[InputInstance]
        inputInstance.tasks.put(currTask, new InputTask(currentOffer._1.getUrl.getAddress.getIp, taskPort.toInt))
        ConnectionRepository.getInstanceService.save(instance.get)
      }
      agentPorts = availablePorts.mkString(",")
      agentPorts.dropRight(1)

      logger.debug(s"Task: $currTask. Ports for task: $ports")
      logger.debug(s"Task: $currTask. Agent ports: $agentPorts")

      val cmd = CommandInfo.newBuilder()
      try {
        val environments = Environment.newBuilder
          .addVariables(Environment.Variable.newBuilder.setName("MONGO_HOST").setValue(params {"mongodbHost"}))
          .addVariables(Environment.Variable.newBuilder.setName("MONGO_PORT").setValue(params {"mongodbPort"}))
          .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(params {"instanceId"}))
          .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(currTask))
          .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(currentOffer._1.getUrl.getAddress.getIp))
          .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(agentPorts))

//        if (instance.moduleType.equals(ModuleConstants.inputStreamingType)) {
//          logger.debug(s"Task: $currTask. Task port: $taskPort")
//          environments.addVariables(Environment.Variable.newBuilder.setName("ENTRY_PORT").setValue(taskPort))
//        }

        cmd
          .addUris(CommandInfo.URI.newBuilder.setValue(getModuleUrl(instance.get)))
          .setValue("java -jar " + jarName)
          .setEnvironment(environments)
      } catch {
        case e: Exception => FrameworkUtil.handleSchedulerException(e, driver, logger)
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
    * Reregistering framework
    */
  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {
    logger.debug(s"New master $masterInfo")
    TasksList.message = s"New master $masterInfo"
  }

  /**
    * Registering framework
    */
  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework as: ${frameworkId.getValue}")
    this.driver = driver

    params = Map(
      ("instanceId", Properties.envOrElse("INSTANCE_ID", "00000000-0000-0000-0000-000000000000")),
      ("mongodbHost", Properties.envOrElse("MONGO_HOST", "127.0.0.1")),
      ("mongodbPort", Properties.envOrElse("MONGO_PORT", "27017"))
    )
    logger.debug(s"Got environment variable: $params")

    instance = ConnectionRepository.getInstanceService.get(params("instanceId"))
    if (instance.isEmpty) {
      logger.error(s"Not found instance")
      driver.stop()
      TasksList.message = "Framework shut down: not found instance."
      return
    }
    logger.debug(s"Got instance ${instance.get.name}")

//    //framework blocking
//    try {
//      val zkHostString = instance.coordinationService.provider.hosts(0)
//      val zkHost = new InetSocketAddress(InetAddress.getByName(zkHostString.split(":")(0)), zkHostString.split(":")(1).toInt)
//      val zkClient = new ZooKeeperClient(Amount.of(1, Time.MINUTES), zkHost)
//      val lockPath = s"/mesos-framework/${instance.coordinationService.namespace}/${params{"instanceId"}}/lock"
//      val dli = new DistributedLockImpl(zkClient, lockPath)
//      dli.lock()
//      logger.info("Framework locked")
//    } catch {
//      case e:Exception => FrameworkUtil.handleSchedulerException(e, driver, logger)
//    }

    perTaskCores = instance.get.perTaskCores
    perTaskMem = instance.get.perTaskRam
    perTaskPortsCount = FrameworkUtil.getCountPorts(instance.get)
    val tasks = if (instance.get.moduleType.equals(ModuleConstants.inputStreamingType))
        (0 until instance.get.parallelism).map(tn => instance.get.name+"-task"+tn)
        else instance.get.executionPlan.tasks.asScala.keys
    tasks.foreach(task => TasksList.newTask(task))
    logger.debug(s"Got tasks: $TasksList")

    try {
      uniqueHosts = Properties.envOrElse("UNIQUE_HOSTS", "false").toBoolean
    } catch {
      case e: Exception =>
    }

    TasksList.message = s"Registered framework as: ${frameworkId.getValue}"
  }


  /**
    * Getting list of offers and tasks count for launch on each slave
    *
    * @param perTaskCores:Double
    * @param perTaskRam:Double
    * @param perTaskPortsCount:Int
    * @param taskCount:Int
    * @param offers:util.List[Offer]
    * @return mutable.ListBuffer[(Offer, Int)]
    */
  def getOffersForSlave(perTaskCores: Double,
                        perTaskRam: Double,
                        perTaskPortsCount: Int,
                        taskCount: Int,
                        offers: util.List[Offer]): mutable.ListBuffer[(Offer, Int)] = {
    var overCpus = 0.0
    var overMem = 0.0
    var overPorts = 0

    val reqCpus = perTaskCores * taskCount
    val reqMem = perTaskRam * taskCount
    val reqPorts = perTaskPortsCount * taskCount

    val tasksCountOnSlave: mutable.ListBuffer[(Offer, Int)] = mutable.ListBuffer()
    for (offer: Offer <- offers.asScala) {
      val portsResource = getResource(offer, "ports")
      var offerPorts = 0
      for (range <- portsResource.getRanges.getRangeList.asScala) {
        overPorts += (range.getEnd - range.getBegin + 1).toInt
        offerPorts += (range.getEnd - range.getBegin + 1).toInt
      }

      tasksCountOnSlave.append(Tuple2(offer, List[Double](
        getResource(offer, "cpus").getScalar.getValue / perTaskCores,
        getResource(offer, "mem").getScalar.getValue / perTaskRam,
        offerPorts / perTaskPortsCount
      ).min.floor.toInt))
      overCpus += getResource(offer, "cpus").getScalar.getValue
      overMem += getResource(offer, "mem").getScalar.getValue
    }
    logger.debug(s"Have resources: $overCpus cpus, $overMem mem, $overPorts ports")
    logger.debug(s"Need resources: $reqCpus cpus, $reqMem mem, $reqPorts ports")
    tasksCountOnSlave
  }

  /**
    * This method give how much resource of type <name> we have on <offer>
    *
    * @param offer:Offer
    * @param name:String
    * @return Double
    */
  def getResource(offer: Offer, name: String): Resource = {
    offer.getResourcesList.asScala.filter(_.getName.equals(name)).head
  }


  /**
    * Filter offered slaves
    * @param offers:util.List[Offer]
    * @param filters:util.Map[String, String]
    * @return util.List[Offer]
    */
  def filterOffers(offers: util.List[Offer], filters: util.Map[String, String]): util.List[Offer] = {
    logger.debug(s"FILTER OFFERS")
    var result: mutable.Buffer[Offer] = mutable.Buffer()
    if (filters != null) {
      for (filter <- filters.asScala) {
        for (offer <- offers.asScala) {
          if (filter._1.matches("""\+.+""")) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.toString.substring(1) == attribute.getName &
                attribute.getText.getValue.matches(filter._2.r.toString)) {
                result += offer
              }
            }
          }
          if (filter._1.matches("""\-.+""")) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.matches(attribute.getName.r.toString) &
                attribute.getText.getValue.matches(filter._2.r.toString)) {
                result = result.filterNot(elem => elem == offer)
              }
            }
          }
        }
      }
    } else result = offers.asScala

    result.asJava
  }

  /**
    * Get jar URI for framework
    * @param instance:Instance
    * @return String
    */
  def getModuleUrl(instance: Instance): String = {
    jarName = configFileService.get("system." + instance.engine).get.value
    val restHost = configFileService.get(ConfigConstants.hostOfCrudRestTag).get.value
    val restPort = configFileService.get(ConfigConstants.portOfCrudRestTag).get.value.toInt
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
  def getPorts(offer: Offer, task: String):Resource = {
    val portsResource = getResource(offer, "ports")
    for (range <- portsResource.getRanges.getRangeList.asScala) {
      availablePortsForOneInstance ++= (range.getBegin to range.getEnd).to[mutable.ListBuffer]
    }

    val ports: mutable.ListBuffer[Long] = availablePortsForOneInstance.take(perTaskPortsCount)
    availablePortsForOneInstance.remove(0, perTaskPortsCount)

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

