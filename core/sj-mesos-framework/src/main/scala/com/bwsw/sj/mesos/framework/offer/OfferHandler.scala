package com.bwsw.sj.mesos.framework.offer

import java.util

import org.apache.log4j.Logger
import org.apache.mesos.Protos._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created by diryavkin_dn on 15.11.16.
  */

/**
  * Object for filter offers.
  */
object OfferHandler {

  private val logger = Logger.getLogger(this.getClass)


  /**
    * Filter offered slaves
    * @param offers:util.List[Offer]
    * @param filters:util.Map[String, String]
    * @return util.List[Offer]
    */
  def filter(offers: util.List[Offer], filters: util.Map[String, String]): util.List[Offer] =
  {
    logger.debug(s"FILTER OFFERS")
    var result: mutable.Buffer[Offer] = mutable.Buffer()
    if (!filters.isEmpty) {
      //todo Дим, подумай, теперь filters не может быть null, мб теперь не понадобится if условие (раньше было условие: filters != null)
      for (filter <- filters.asScala) {
        for (offer <- offers.asScala) {
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
    } else result = offers.asScala

    result.asJava
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
      val portsResource = OfferHandler.getResource(offer, "ports")
      var offerPorts = 0
      for (range <- portsResource.getRanges.getRangeList.asScala) {
        overPorts += (range.getEnd - range.getBegin + 1).toInt
        offerPorts += (range.getEnd - range.getBegin + 1).toInt
      }

      val cpus = OfferHandler.getResource(offer, "cpus").getScalar.getValue
      val mem = OfferHandler.getResource(offer, "mem").getScalar.getValue

      tasksCountOnSlave.append(Tuple2(offer, List[Double](
        cpus / perTaskCores,
        mem / perTaskRam,
        offerPorts / perTaskPortsCount
      ).min.floor.toInt))
      overCpus += cpus
      overMem += mem
    }
    logger.debug(s"Have resources: $overCpus cpus, $overMem mem, $overPorts ports")
    logger.debug(s"Need resources: $reqCpus cpus, $reqMem mem, $reqPorts ports")
    tasksCountOnSlave
  }

}
