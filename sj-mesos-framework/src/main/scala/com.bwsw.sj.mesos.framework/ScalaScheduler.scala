package com.bwsw.sj.mesos.framework

import java.io.{PrintWriter, StringWriter}
import java.util
import com.bwsw.sj.common.DAL.model.module.Instance
import org.apache.mesos.Protos._
import org.apache.mesos.{Scheduler, SchedulerDriver}
import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.util.Properties
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
import com.twitter.common.quantity.{Amount, Time}
import java.net.{InetAddress, InetSocketAddress, URI}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import org.apache.log4j.Logger


class ScalaScheduler extends Scheduler {

  var driver: SchedulerDriver = null
  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0
  var params = immutable.Map[String, String]()
  private val logger = Logger.getLogger(getClass)
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
    */
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    // TODO : REMOVE THIS
    logger.info(s"RESOURCE OFFERS")

    /** set availablePortsForOneInstance to empty for new Instance */
    availablePortsForOneInstance.remove(0, availablePortsForOneInstance.length)

    for (offer <- offers.asScala) {
      logger.info(s"PORTS RESOURCE: ${offer.getResources(3)}")
    }




    // TODO : add filter
    // val filteredOffers = filterOffers(offers, this.instance.nodeAttributes)
    val filteredOffers = offers
    if (filteredOffers.size == 0) {
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      TasksList.message = "No one node selected"
      return
    }

    for (offer <- filteredOffers.asScala) {
      logger.debug(s"Offer ID: ${offer.getId.getValue}")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}")
    }

    val tasksCount = TasksList.toLaunch.size
    var tasksOnSlaves = howMuchTasksOnSlave(this.perTaskCores, this.perTaskMem, this.perTaskPortsCount, tasksCount, filteredOffers)

    var overTasks = 0
    for (slave <- tasksOnSlaves) {overTasks += slave._2}
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
    for (curr_task <- TasksList.toLaunch) {

      val currentOffer = tasksOnSlaves(offerNumber)
      if (offerNumber >= tasksOnSlaves.size - 1) {
        offerNumber = 0
      } else {
        offerNumber += 1
      }



      // Task Resources
      val cpus = Resource.newBuilder.
        setType(org.apache.mesos.Protos.Value.Type.SCALAR).
        setName("cpus").
        setScalar(org.apache.mesos.Protos.Value.
          Scalar.newBuilder.setValue(this.perTaskCores)).
        build
      val mem = Resource.newBuilder.
        setType(org.apache.mesos.Protos.Value.Type.SCALAR).
        setName("mem").
        setScalar(org.apache.mesos.Protos.Value.
          Scalar.newBuilder.setValue(this.perTaskMem)).
        build
      val ports = getPorts(currentOffer._1, curr_task)

      var agentPorts:String = ""
      ports.getRanges.getRangeList.asScala.foreach(agentPorts += _.getBegin.toString+",")
      agentPorts.dropRight(1)
      logger.info(s"PORTS: $ports")
      logger.info(s"AGENT PORTS: $agentPorts")



      val cmd = CommandInfo.newBuilder()
      try {
         cmd
          .addUris(CommandInfo.URI.newBuilder.setValue(getModuleUrl(this.instance)))
          .setValue("java -jar " + jarName)
//             .setValue("sh testScript.sh")
          .setEnvironment(Environment.newBuilder
            .addVariables(Environment.Variable.newBuilder.setName("MONGO_HOST").setValue(params {
              "mongodbHost"
            }))
            .addVariables(Environment.Variable.newBuilder.setName("MONGO_PORT").setValue(params {
              "mongodbPort"
            }))
            .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(params {
              "instanceId"
            }))
            .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(curr_task))
            .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(currentOffer._1.getHostname))
            .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(agentPorts))
          )
      } catch {
        case e: Exception => handleSchedulerException(e)
      }



      while (tasksOnSlaves(offerNumber)._2 == 0) {
        tasksOnSlaves = tasksOnSlaves.filterNot(elem => elem == tasksOnSlaves(offerNumber))
        if (offerNumber > tasksOnSlaves.size - 1) {
          offerNumber = 0
        }
      }


      logger.info(s"Current task: $curr_task")
      logger.info(s"Current slave: ${currentOffer._1.getSlaveId.getValue}")

      val task = TaskInfo.newBuilder
        .setCommand(cmd)
        .setName(curr_task)
        .setTaskId(TaskID.newBuilder.setValue(curr_task))
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

      tasksOnSlaves.updated(tasksOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
      TasksList.launched(curr_task)

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

    this.params = Map(
      ("instanceId", Properties.envOrElse("INSTANCE_ID", "00000000-0000-0000-0000-000000000000")),
      ("mongodbHost", Properties.envOrElse("MONGO_HOST", "127.0.0.1")),
      ("mongodbPort", Properties.envOrElse("MONGO_PORT", "27017"))
    )
    logger.debug(s"Got environment variable: ${this.params}")

    this.instance = ConnectionRepository.getInstanceService.get(this.params("instanceId"))
    if (this.instance == null) {
      logger.error(s"Not found instance")
      driver.stop()
      TasksList.message = "Framework shut down: not found instance."
      return
    }
    this.perTaskCores = instance.perTaskCores
    this.perTaskMem = instance.perTaskRam
    instance.moduleType match {
      case "output-streaming" => perTaskPortsCount = 1
      case "regular-streaming" => perTaskPortsCount = instance.inputs.length+instance.outputs.length+4
    }
    logger.info(s"Got instance")
    logger.debug(s"${this.instance}")

    try {
      val zkHost = this.instance.coordinationService.provider.hosts(0)
      val zkhost = new InetSocketAddress(InetAddress.getByName(zkHost.split(":")(0)), zkHost.split(":")(1).toInt)
      val zkClient = new ZooKeeperClient(Amount.of(1, Time.MINUTES), zkhost)
      val lockPath = s"/mesos-framework/${this.instance.coordinationService.namespace}/${this.params{"instanceId"}}/lock"
      val dli = new DistributedLockImpl(zkClient, lockPath)
      dli.lock()
      logger.info("Framework locked")
    } catch {
      case e:Exception => handleSchedulerException(e)
    }

    val tasks = instance.executionPlan.tasks
    logger.info(s"Got tasks")
    for (task <- tasks.asScala) {
      TasksList.newTask(task._1, task._2.inputs.asScala)
      logger.info(s"$task")
    }
    TasksList.message = s"Registered framework as: ${frameworkId.getValue}"
  }



  /**
    * This method give list of offer and how many tasks we can launch on each slave.
    */
  def howMuchTasksOnSlave(perTaskCores: Double, perTaskRam: Double, perTaskPortsCount: Int, offers: util.List[Offer]): List[Tuple2[Offer, Int]] = {
    var over_cpus = 0.0
    var over_mem = 0.0
    var over_ports = 0

    val req_cpus = perTaskCores * instance.parallelism
    val req_mem = perTaskRam * instance.parallelism
    val req_ports = perTaskPortsCount * instance.parallelism



    var tasksNumber: List[Tuple2[Offer, Int]] = List()
    for (offer <- offers.asScala) {
      val portsResource = getPortsResource(offer)
      var offerPorts = 0
      for (range <- portsResource.getRanges.getRangeList.asScala){
        over_ports += (range.getEnd-range.getBegin+1).toInt
        offerPorts += (range.getEnd-range.getBegin+1).toInt
      }
      tasksNumber = tasksNumber.:::(List(Tuple2(

        offer, List[Double](
          getResource(offer, "cpus") / perTaskCores,
          getResource(offer, "mem") / perTaskRam,
          offerPorts / perTaskPortsCount
        ).min.floor.toInt
      )))
      over_cpus += getResource(offer, "cpus")
      over_mem += getResource(offer, "mem")
    }
    logger.info(s"Have resources: $over_cpus cpus, $over_mem mem, $over_ports ports")
    logger.info(s"Required resources: $req_cpus cpus, $req_mem mem, $req_ports ports")
    tasksNumber
  }




  def getResource(offer: Offer, name: String): Double = {
    /**
      * This method give how much resource of type <name> we have on <offer>
      */
    val res = offer.getResourcesList
    for (r <- res.asScala if r.getName == name) {
      return r.getScalar.getValue
    }
    0.0
  }




  def filterOffers(offers: util.List[Offer], filters: java.util.Map[String, String]): util.List[Offer] = {
    /**
      * Filter offered slaves
      */
    logger.info(s"FILTER OFFERS")
    var result: List[Offer] = List()
    if (filters != null) {
      for (offer <- offers.asScala) {
        for (filter <- filters.asScala) {
          if (filter._1.matches("""\+.+""".r.toString)) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.toString.substring(1) == attribute.getName &
              attribute.getText.getValue.matches(filter._2.r.toString)) {
                result ::= offer
              }
            }
          }
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
    */
  def getModuleUrl(instance: Instance): String = {
    // TODO:return back get host
    lazy val restHost = "192.168.1.174" // configFileService.get(ConfigConstants.hostOfCrudRestTag).value
    lazy val restPort = 8000 // configFileService.get(ConfigConstants.portOfCrudRestTag).value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/").toString
    jarName = configFileService.get("system." + instance.engine).value
    logger.info(s"URI: ${restAddress + jarName}")
    restAddress + jarName
//    "http://192.168.1.225:8000/testScript.sh"
  }


  def getPortsResource(offer:Offer): Resource = {
    var portsResource: Resource = Resource.newBuilder
      .setName("ports")
      .setType(Value.Type.RANGES)
      .build
    for (resource <- offer.getResourcesList.asScala) {
      if (resource.getName == "ports") {
        portsResource = resource
      }
    }
    portsResource
  }



  /**
    * Get random unused ports
    */
  def getPorts(offer:Offer, task:String):Resource = {
    val portsResource: Resource = getPortsResource(offer)
    for (range <- portsResource.getRanges.getRangeList.asScala) {
      availablePortsForOneInstance ++= (range.getBegin to range.getEnd).to[collection.mutable.ListBuffer]
    }

    val agentPorts: collection.mutable.ListBuffer[Long] = availablePortsForOneInstance.take(this.perTaskPortsCount)
    availablePortsForOneInstance.remove(0, this.perTaskPortsCount)

    val ranges = Value.Ranges.newBuilder
    for (port <- agentPorts) {
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
    *
    */
  def handleSchedulerException(e:Exception) = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.message = e.getMessage
    logger.info(s"FRAMEWORK GOT EXCEPTION: ${sw.toString}")
    driver.stop()
    System.exit(1)
  }
}

