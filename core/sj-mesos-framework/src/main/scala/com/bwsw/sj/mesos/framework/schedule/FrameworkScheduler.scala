package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}
import java.net.{InetAddress, InetSocketAddress, URI}
import java.util

import com.bwsw.sj.common.DAL.model.module.{InputInstance, Instance}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.{ConfigConstants, ModuleConstants}
import com.bwsw.sj.mesos.framework.task.{StatusHandler, TasksList}
import com.twitter.common.quantity.{Amount, Time}
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
import org.apache.log4j.Logger
import org.apache.mesos.Protos._
import org.apache.mesos.{Scheduler, SchedulerDriver}

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.collection.mutable
import scala.util.Properties


/**
  *
  * Created: 10/05/2016
  *
  * @author Dmitry Diryavkin
  */
//todo написать понятные комментарии к методам (scaladocs) и классам
class FrameworkScheduler extends Scheduler {

  private val logger = Logger.getLogger(this.getClass)

  var driver: SchedulerDriver = null
  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0
  var params = immutable.Map[String, String]()
  var instance: Instance = null
  val configFileService = ConnectionRepository.getConfigService
  var jarName: String = null
  val availablePortsForOneInstance: collection.mutable.ListBuffer[Long] = collection.mutable.ListBuffer()

  def error(driver: SchedulerDriver, message: String) {
    logger.error(s"Got error message: $message")
    TasksList.message = s"Got error message: $message"
  }

  def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int) {}

  def slaveLost(driver: SchedulerDriver, slaveId: SlaveID) {}

  def disconnected(driver: SchedulerDriver) {}

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]) {
    logger.info(s"Got framework message")
    logger.debug(s"$data")
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
    * @param driver
    * @param offers
    */
  //todo сюда приходят offers с cpu=0.0 и mem=0.0, то есть нет ресурсов для запуска таски
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    // TODO : REMOVE THIS
    logger.debug(s"RESOURCE OFFERS")

    availablePortsForOneInstance.remove(0, availablePortsForOneInstance.length)

    for (offer <- offers.asScala) {
      logger.debug(s"PORTS RESOURCE: ${offer.getResources(3)}")
    }


    // TODO : add filter
    val filteredOffers = filterOffers(offers, this.instance.nodeAttributes)
//    val filteredOffers = offers
    if (filteredOffers.size == 0) {
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      TasksList.message = "No one node selected"
      logger.info("Return from resourceOffers")
      return
    }

    for (offer <- filteredOffers.asScala) {
      logger.debug(s"Offer ID: ${offer.getId.getValue}")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}")
    }

    val tasksCount = TasksList.toLaunch.size
    var tasksOnSlaves = getOffersForSlave(perTaskCores,
      perTaskMem,
      perTaskPortsCount,
      tasksCount,
      filteredOffers)

    var overTasks = 0
    for (slave <- tasksOnSlaves) {
      overTasks += slave._2
    }

    logger.info(s"Count tasks can be launched: $overTasks")
    logger.info(s"Count tasks must be launched: $tasksCount")

    if (tasksCount > overTasks) {
      logger.info(s"Can not launch tasks: no required resources")
      TasksList.message = "Can not launch tasks: no required resources"
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      return
    }

    logger.info(s"Tasks to launch: ${TasksList.toLaunch}")

    var offerNumber = 0
    var launchedTasks: Map[OfferID, List[TaskInfo]] = Map()
    for (currTask <- TasksList.toLaunch) {

      val currentOffer = tasksOnSlaves(offerNumber)
      if (offerNumber >= tasksOnSlaves.size - 1) {
        offerNumber = 0
      } else {
        offerNumber += 1
      }


      // Task Resources
      //todo org.apache.mesos.Protos.Value >> Value, т.к. ранее выполнен импорт org.apache.mesos.Protos._
      //можно скорректировать импорт  на "org.apache.mesos", тогда понятнее будет, что Value берется в Protos
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
      if (!instance.moduleType.equals(ModuleConstants.inputStreamingType)) {
        ports.getRanges.getRangeList.asScala.map(_.getBegin.toString).mkString(",")
      } else {
        val availPorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)
        taskPort = availPorts.head
        agentPorts = availPorts.remove(0).mkString(",")
      }
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
          .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(currentOffer._1.getHostname))
          .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(agentPorts))

        if (instance.moduleType.equals(ModuleConstants.inputStreamingType)) {
          logger.debug(s"Task: $currTask. Task port: $taskPort")
          environments.addVariables(Environment.Variable.newBuilder.setName("ENTRY_PORT").setValue(taskPort))
        }

        cmd
          .addUris(CommandInfo.URI.newBuilder.setValue(getModuleUrl(instance)))
          .setValue("java -jar " + jarName)
          .setEnvironment(environments)
      } catch {
        case e: Exception => handleSchedulerException(e)
      }

      while (tasksOnSlaves(offerNumber)._2 == 0) {
        tasksOnSlaves = tasksOnSlaves.filterNot(elem => elem == tasksOnSlaves(offerNumber))
        if (offerNumber > tasksOnSlaves.size - 1) {
          offerNumber = 0
        }
      }

      logger.info(s"Task: $currTask.")
      logger.info(s"Task: $currTask. Current slave: ${currentOffer._1.getSlaveId.getValue}")

      val task = TaskInfo.newBuilder
        .setCommand(cmd)
        .setName(currTask)
        .setTaskId(TaskID.newBuilder.setValue(currTask))
        .addResources(cpus)
        .addResources(mem)
        .addResources(ports)
        .setSlaveId(currentOffer._1.getSlaveId)
        .build()
      var listTasks: List[TaskInfo] = List()
      listTasks ::= task

      if (launchedTasks.contains(currentOffer._1.getId)) {
        launchedTasks += (currentOffer._1.getId -> launchedTasks {
          currentOffer._1.getId
        }.:::(listTasks))
      } else {
        launchedTasks += (currentOffer._1.getId -> listTasks)
      }

      // update how much tasks we can run on slave when launch current task
      //todo убрать строку? идея ее подчеркивает, как useless expression
      tasksOnSlaves.updated(tasksOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
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
    logger.info(s"New master $masterInfo")
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
    logger.debug(s"Got environment variable: ${this.params}")

    this.instance = ConnectionRepository.getInstanceService.get(this.params("instanceId"))
    if (instance == null) {
      logger.error(s"Not found instance")
      driver.stop()
      TasksList.message = "Framework shut down: not found instance."
      return
    }
    perTaskCores = instance.perTaskCores
    perTaskMem = instance.perTaskRam

    perTaskPortsCount = FrameworkUtil.getCountPorts(instance)
    logger.info(s"Got instance")
    logger.debug(s"$instance")

    try {
      val zkHost = instance.coordinationService.provider.hosts(0)
      val zkhost = new InetSocketAddress(InetAddress.getByName(zkHost.split(":")(0)), zkHost.split(":")(1).toInt)
      val zkClient = new ZooKeeperClient(Amount.of(1, Time.MINUTES), zkhost)
      val lockPath = s"/mesos-framework/${instance.coordinationService.namespace}/${params{"instanceId"}}/lock"//todo change to URI
      val dli = new DistributedLockImpl(zkClient, lockPath)
      dli.lock()
      logger.info("Framework locked")
    } catch {
      case e:Exception => handleSchedulerException(e)
    }

    if (instance.moduleType.equals(ModuleConstants.inputStreamingType)) { //todo у нас есть константы!
      val tasks = instance.asInstanceOf[InputInstance].tasks
      logger.info(s"Got tasks")
      for (task <- tasks.asScala) {
        TasksList.newTask(task._1)
        logger.info(s"${task._1}")
      }
    } else {
      val tasks = instance.executionPlan.tasks
      logger.info(s"Got tasks")
      for (task <- tasks.asScala) {
        TasksList.newTask(task._1)
        logger.info(s"${task._1}")
      }
    }
    TasksList.message = s"Registered framework as: ${frameworkId.getValue}"
  }

  /**
    * Getting list of offers and tasks count for lunch on each slave
    *
    * @param perTaskCores
    * @param perTaskRam
    * @param perTaskPortsCount
    * @param taskCount
    * @param offers
    * @return
    */
  def getOffersForSlave(perTaskCores: Double,
                        perTaskRam: Double,
                        perTaskPortsCount: Int,
                        taskCount: Int,
                        offers: util.List[Offer]): List[(Offer, Int)] = {
    var overCpus = 0.0
    var overMem = 0.0
    var overPorts = 0

    val reqCpus = perTaskCores * taskCount
    val reqMem = perTaskRam * taskCount
    val reqPorts = perTaskPortsCount * taskCount

    var tasksNumber: List[(Offer, Int)] = List()
    for (offer: Offer <- offers.asScala) {
      val portsResource = getPortsResource(offer)
      var offerPorts = 0
      for (range <- portsResource.getRanges.getRangeList.asScala) {
        overPorts += (range.getEnd - range.getBegin + 1).toInt
        offerPorts += (range.getEnd - range.getBegin + 1).toInt
      }

      tasksNumber = tasksNumber.:::(List(Tuple2(
        offer, List[Double](
          getResource(offer, "cpus") / perTaskCores,
          getResource(offer, "mem") / perTaskRam,
          offerPorts / perTaskPortsCount
        ).min.floor.toInt
      )))
      overCpus += getResource(offer, "cpus")
      overMem += getResource(offer, "mem")
    }
    logger.info(s"Have resources: $overCpus cpus, $overMem mem, $overPorts ports")
    logger.info(s"Required resources: $reqCpus cpus, $reqMem mem, $reqPorts ports")
    tasksNumber
  }

  /**
    * This method give how much resource of type <name> we have on <offer>
    */
  def getResource(offer: Offer, name: String): Double = {
    val res = offer.getResourcesList
    for (r <- res.asScala if r.getName == name) {
      return r.getScalar.getValue
    }
    0.0
  }

  /**
    * Filter offered slaves
    * @param offers
    * @param filters
    * @return
    */
  //todo оптимизировать if'ы
  def filterOffers(offers: util.List[Offer], filters: util.Map[String, String]): util.List[Offer] = {
    logger.info(s"FILTER OFFERS")
    var result: List[Offer] = List()
    if (filters != null) { //todo эта проверка точно нужна? Если filters=null, то цикл просто не выполнится (проверить, что asScala не инициирует NPE)
      for (filter <- filters.asScala) {
        for (offer <- offers.asScala) {
          if (filter._1.matches("""\+.+""".r.toString)) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.toString.substring(1) == attribute.getName &
                attribute.getText.getValue.matches(filter._2.r.toString)) {

                result ::= offer
              }
            }
          }
          //todo зачем такая сложная конструкция <"""\-.+""".r.toString>? Выражения <"""\-.+""".r.toString> == <"""\-.+""">
          if (filter._1.matches("""\-.+""".r.toString)) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.matches(attribute.getName.r.toString) &
                attribute.getText.getValue.matches(filter._2.r.toString)) {

                result = result.filterNot(elem => elem == offer)
              }
            }
          }
        }
      }
    } else return offers
    result.asJava
  }

  /**
    * Get jar URI for framework
    * @param instance
    * @return
    */
  def getModuleUrl(instance: Instance): String = {
    // TODO:return back get host
    jarName = configFileService.get("system." + instance.engine).value
    val restHost = configFileService.get(ConfigConstants.hostOfCrudRestTag).value
    val restPort = configFileService.get(ConfigConstants.portOfCrudRestTag).value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/jars/$jarName").toString
    logger.info(s"Engine downloading URL: $restAddress")
    restAddress
//    "http://192.168.1.225:8000/testScript.sh"
  }

  /**
    *
    * @param offer
    * @return
    */
  def getPortsResource(offer: Offer): Resource = {
    var portsResource: Resource = Resource.newBuilder
      .setName("ports")
      .setType(Value.Type.RANGES)
      .build

    for (resource <- offer.getResourcesList.asScala) {
      if (resource.getName == "ports") {
        portsResource = resource
      }
    }
    //todo а нельзя ли сделать так? Суть та же, но код короче
    //portsResource = offer.getResourcesList.asScala.filter(_.getName.equals("ports")).head

    portsResource
  }

  /**
    * Get random unused ports
    */
  def getPorts(offer: Offer, task: String):Resource = {
    val portsResource: Resource = getPortsResource(offer)
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

  /**
    * Handler for Scheduler Exception
    */
  def handleSchedulerException(e: Exception) = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.message = e.getMessage
    logger.error(s"Framework error: ${sw.toString}")
    driver.stop()
    System.exit(1)
  }
}

