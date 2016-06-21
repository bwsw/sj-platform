package com.bwsw.sj.custom.regular.utils

import com.bwsw.common.ObjectSerializer

object SflowParser {

  private val objectSerializer = new ObjectSerializer()
  private val fieldNames = Array("name", "agentAddress", "inputPort", "outputPort", "srcMAC",
    "dstMAC", "ethernetType", "inVlan", "outVlan", "srcIP", "dstIP", "ipProtocol", "ipTos", "ipTtl",
    "udpSrcPort", "udpDstPort", "tcpFlags", "packetSize", "ipSize", "samplingRate")

  def parse(serializedSflow: Array[Byte]) = {
    objectSerializer.deserialize(serializedSflow).asInstanceOf[String].split(",").zip(fieldNames).toMap
  }
}
