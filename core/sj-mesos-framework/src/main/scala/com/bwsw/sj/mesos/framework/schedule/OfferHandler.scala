package com.bwsw.sj.mesos.framework.schedule

import java.util

import com.bwsw.sj.mesos.framework.task.TasksList
import org.apache.log4j.Logger
import org.apache.mesos.Protos._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Object for filter offers.
 */
object OfferHandler {
  private val logger = Logger.getLogger(this.getClass)
  var filteredOffers = mutable.Buffer[Offer]()
  var offerNumber: Int = 0
  private var offers = mutable.Buffer[Offer]()

  /**
   * Filter offered slaves
   *
   * @param filters:util.Map[String, String]
   * @return util.List[Offer]
   */
  def filter(filters: util.Map[String, String]) = {
    logger.debug(s"FILTER OFFERS.")
    var result: mutable.Buffer[Offer] = mutable.Buffer()
    if (!filters.isEmpty) {
      for (filter <- filters.asScala) {
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
   * Getting list of offers and count tasks for launch on each slave
   *
   * @return mutable.ListBuffer[(Offer, Int)]
   */
  def getOffersForSlave(): mutable.ListBuffer[(Offer, Int)] = {
    var overCpus = 0.0
    var overMem = 0.0
    var overPorts = 0

    val reqCpus = TasksList.perTaskCores * TasksList.count
    val reqMem = TasksList.perTaskMem * TasksList.count
    val reqPorts = TasksList.perTaskPortsCount * TasksList.count

    val tasksCountOnSlave: mutable.ListBuffer[(Offer, Int)] = mutable.ListBuffer()
    for (offer: Offer <- OfferHandler.filteredOffers) {
      val portsResource = OfferHandler.getResource(offer, "ports")
      var ports = 0
      for (range <- portsResource.getRanges.getRangeList.asScala) {
        overPorts += (range.getEnd - range.getBegin + 1).toInt
        ports += (range.getEnd - range.getBegin + 1).toInt
      }

      val cpus = OfferHandler.getResource(offer, "cpus").getScalar.getValue
      val mem = OfferHandler.getResource(offer, "mem").getScalar.getValue

      tasksCountOnSlave.append(Tuple2(offer, List[Double](
        cpus / TasksList.perTaskCores,
        mem / TasksList.perTaskMem,
        ports / TasksList.perTaskPortsCount
      ).min.floor.toInt))
      overCpus += cpus
      overMem += mem
    }
    logger.debug(s"Have resources: $overCpus cpus, $overMem mem, $overPorts ports.")
    logger.debug(s"Need resources: $reqCpus cpus, $reqMem mem, $reqPorts ports.")
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

  def updateOfferNumber(tasksOnSlaves: mutable.ListBuffer[(Offer, Int)]): mutable.ListBuffer[(Offer, Int)] = {
    var result: mutable.ListBuffer[(Offer, Int)] = tasksOnSlaves
    while (tasksOnSlaves(offerNumber)._2 == 0) {
      result = tasksOnSlaves.filterNot(_ == tasksOnSlaves(offerNumber))
      if (offerNumber > tasksOnSlaves.size - 1) offerNumber = 0
    }
    result
  }
}
