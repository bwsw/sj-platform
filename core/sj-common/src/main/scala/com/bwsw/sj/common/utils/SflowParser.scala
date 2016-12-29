package com.bwsw.sj.common.utils

object SflowParser {

  def parse(serializedSflow: Array[Byte]): Option[SflowRecord] = {
    val maybeSflow = new String(serializedSflow).split(",")
    if (maybeSflow.length == SflowRecord.countOfParameters) {
      Some(SflowRecord(maybeSflow))
    } else None
  }
}

case class SflowRecord(timestamp: Long, name: String, agentAddress: String, inputPort: Int, outputPort: Int,
                       srcMAC: String, dstMAC: String, ethernetType: String, inVlan: Int, outVlan: Int,
                       srcIP: String, dstIP: String, ipProtocol: Int, ipTos: String, ipTtl: Int, udpSrcPort: Int,
                       udpDstPort: Int, tcpFlags: String, packetSize: Int, ipSize: Int, samplingRate: Int) {
}

object SflowRecord {
  val countOfParameters = 21

  def apply(parameters: Array[String]): SflowRecord = {
    SflowRecord(parameters(0).toLong, parameters(1), parameters(2), parameters(3).toInt, parameters(4).toInt,
      parameters(5), parameters(6), parameters(7), parameters(8).toInt, parameters(9).toInt,
      parameters(10), parameters(11), parameters(12).toInt, parameters(13), parameters(14).toInt, parameters(15).toInt,
      parameters(16).toInt, parameters(17), parameters(18).toInt, parameters(19).toInt, parameters(20).toInt)
  }
}