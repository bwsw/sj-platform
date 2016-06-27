package com.bwsw.sj.custom.regular.utils

object SflowParser {

  private val fieldNames = Array("name", "agentAddress", "inputPort", "outputPort", "srcMAC",
    "dstMAC", "ethernetType", "inVlan", "outVlan", "srcIP", "dstIP", "ipProtocol", "ipTos", "ipTtl",
    "udpSrcPort", "udpDstPort", "tcpFlags", "packetSize", "ipSize", "samplingRate")

  def parse(serializedSflow: Array[Byte])= {
    val maybeSflow = new String(serializedSflow).split(",")
    if (maybeSflow.length == fieldNames.length) Some(fieldNames.zip(maybeSflow).toMap)
    else None
  }
}
