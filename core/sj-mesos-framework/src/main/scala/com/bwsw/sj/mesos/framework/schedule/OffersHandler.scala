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

import com.bwsw.sj.mesos.framework.task.TasksList
import org.apache.log4j.Logger
import org.apache.mesos.Protos._
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Object to filter offers.
 */
object OffersHandler {
  private val logger = Logger.getLogger(this.getClass)
  var filteredOffers = mutable.Buffer[Offer]()
  var offerNumber: Int = 0
  private var offers = mutable.Buffer[Offer]()
  var tasksCountOnSlaves: mutable.ListBuffer[(Offer, Int)] = mutable.ListBuffer[(Offer, Int)]()


  def getOffers = offers

  /**
   * Filters offered slaves
   *
   * @param filters:util.Map[String, String]
   * @return util.List[Offer]
   */
  def filter(filters: Map[String, String]): Unit = {
    logger.info(s"Filtering resource offers")
    var result: mutable.Buffer[Offer] = mutable.Buffer()
    if (filters.nonEmpty) {
      for (filter <- filters) {
        for (offer <- offers) {
          if (filter._1.matches( """\+.+""")) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.toString.substring(1) == attribute.getName &
                attribute.getText.getValue.matches(filter._2.r.toString)) {
                result += offer
              }
            }
          }
          if (filter._1.matches( """\-.+""")) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.matches(attribute.getName.r.toString) &
                attribute.getText.getValue.matches(filter._2.r.toString)) {
                result = result.filterNot(elem => elem == offer)
              }
            }
          }
        }
      }
    } else result = offers
    filteredOffers = result
    tasksCountOnSlaves = getTasksForOffer
  }

  /**
   * Returns amount of <name> resource in <offer>
   *
   * @param offer:Offer
   * @param name:String
   * @return Double
   */
  def getResource(offer: Offer, name: String): Resource = {
    offer.getResourcesList.asScala.filter(_.getName.equals(name)).head
  }

  /**
   * Returns list of offers and counts tasks to launch on each slave.
   *
   * @return mutable.ListBuffer[(Offer, Int)]
   */
  def getTasksForOffer: mutable.ListBuffer[(Offer, Int)] = {
    logger.info("Calculating how much tasks can be launched on selected offers for this instance")
    var overCpus = 0.0
    var overMem = 0.0
    var overPorts = 0

    val reqCpus = TasksList.perTaskCores * TasksList.count
    val reqMem = TasksList.perTaskMem * TasksList.count
    val reqPorts = TasksList.perTaskPortsCount * TasksList.count

    val tasksCountOnSlave: mutable.ListBuffer[(Offer, Int)] = mutable.ListBuffer()
    for (offer: Offer <- OffersHandler.filteredOffers) {
      val portsResource = OffersHandler.getResource(offer, "ports")
      var ports = 0
      for (range <- portsResource.getRanges.getRangeList.asScala) {
        overPorts += (range.getEnd - range.getBegin + 1).toInt
        ports += (range.getEnd - range.getBegin + 1).toInt
      }

      val cpus = OffersHandler.getResource(offer, "cpus").getScalar.getValue
      val mem = OffersHandler.getResource(offer, "mem").getScalar.getValue

      tasksCountOnSlave.append(Tuple2(offer, List[Double](
        cpus / TasksList.perTaskCores,
        mem / TasksList.perTaskMem,
        ports / TasksList.perTaskPortsCount
      ).min.floor.toInt))
      overCpus += cpus
      overMem += mem
    }
    tasksCountOnSlave
  }

  def getOfferIp(offer: Offer) = {
    offer.getUrl.getAddress.getIp
  }

  def setOffers(offers: mutable.Buffer[Offer]) = {
    this.offers = offers
  }

  def getNextOffer(tasksOnSlaves: mutable.ListBuffer[(Offer, Int)]): (Offer, Int) = {

    val offer = tasksOnSlaves(offerNumber)
    if (offerNumber >= tasksOnSlaves.size - 1) {
      offerNumber = 0
    }
    else {
      offerNumber += 1
    }
    if (offers.contains(offer._1)) {
      offers.remove(offers.indexOf(offer._1))
    }
    offer
  }

  /**
    * Update number of available offers
    * @param tasksOnSlaves How much tasks can be launched on each slaves
    * @return New list of offers, without empty
    */
  def updateOfferNumber(tasksOnSlaves: mutable.ListBuffer[(Offer, Int)]): mutable.ListBuffer[(Offer, Int)] = {
    var result: mutable.ListBuffer[(Offer, Int)] = tasksOnSlaves
    while (result.length > 1 && tasksOnSlaves(offerNumber)._2 == 0) {
      result = tasksOnSlaves.filterNot(_ == tasksOnSlaves(offerNumber))
      if (offerNumber > result.size - 1) offerNumber = 0
    }
    result
  }

  /**
    * Returns random free ports.
 *
    * @param offer:Offer
    * @return Resource
    */
  def getPortsResource(offer: Offer): Resource = {
    val portsResource = OffersHandler.getResource(offer, "ports")
    for (range <- portsResource.getRanges.getRangeList.asScala) {
      TasksList.addAvailablePorts((range.getBegin to range.getEnd).to[mutable.ListBuffer])
    }

    val ports: mutable.ListBuffer[Long] = TasksList.getAvailablePorts.take(TasksList.perTaskPortsCount)
    TasksList.getAvailablePorts.remove(0, TasksList.perTaskPortsCount)

    val ranges = Value.Ranges.newBuilder
    for (port <- ports) {
      ranges.addRange(Value.Range.newBuilder.setBegin(port).setEnd(port))
    }

    Resource.newBuilder
      .setName("ports")
      .setType(Value.Type.RANGES)
      .setRanges(ranges)
      .build()
  }

  def getCpusResource: Resource = {
    Resource.newBuilder
      .setType(Value.Type.SCALAR)
      .setName("cpus")
      .setScalar(Value.Scalar.newBuilder.setValue(TasksList.perTaskCores))
      .build
  }

  def getMemResource: Resource = {
    Resource.newBuilder
      .setType(org.apache.mesos.Protos.Value.Type.SCALAR)
      .setName("mem")
      .setScalar(org.apache.mesos.Protos.Value.Scalar.newBuilder.setValue(TasksList.perTaskMem))
      .build
  }

  /**
    * Distribution of tasks by nodes, where each task must be launched
    * @param injector
    */
  def distributeTasksOnSlaves()(implicit injector: Injector): Unit = {
    logger.info(s"Distribute tasks to resource offers")
    OffersHandler.offerNumber = 0
    for (currTask <- TasksList.toLaunch) {
      createTaskToLaunch(currTask, OffersHandler.tasksCountOnSlaves)
      OffersHandler.tasksCountOnSlaves = OffersHandler.updateOfferNumber(OffersHandler.tasksCountOnSlaves)
    }
  }

  /**
    * Create task to launch, update tasks count on slaves
    * Launches tasks and returns their names
    *
    * @param taskId
    * @param tasksCountOnSlaves How much tasks on slave
    * @return Launched tasks list
    */
  private def createTaskToLaunch(taskId: String, tasksCountOnSlaves: mutable.ListBuffer[(Offer, Int)])(implicit injector: Injector): ListBuffer[String] = {
    val currentOffer = OffersHandler.getNextOffer(tasksCountOnSlaves)
    val task = TasksList.createTaskToLaunch(taskId, currentOffer._1)

    TasksList.addTaskToSlave(task, currentOffer)

    // update how much tasks we can run on slave when launch current task
    tasksCountOnSlaves.update(tasksCountOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
    TasksList.launched(taskId)
  }
}
