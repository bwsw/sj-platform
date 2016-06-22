package com.bwsw.sj.custom.regular.utils

object SflowParser {

  private val fieldNames = Array("name", "agentAddress", "inputPort", "outputPort", "srcMAC",
    "dstMAC", "ethernetType", "inVlan", "outVlan", "srcIP", "dstIP", "ipProtocol", "ipTos", "ipTtl",
    "udpSrcPort", "udpDstPort", "tcpFlags", "packetSize", "ipSize", "samplingRate")

  def parse(serializedSflow: Array[Byte]) = {
    fieldNames.zip(new String(serializedSflow).split(",")).toMap
  }
}
