package com.bwsw.sj.custom.regular.utils

object SflowParser {

  private val fieldNames = Array("ts","name", "agentAddress", "inputPort", "outputPort", "srcMAC",
    "dstMAC", "ethernetType", "inVlan", "outVlan", "srcIP", "dstIP", "ipProtocol", "ipTos", "ipTtl",
    "udpSrcPort", "udpDstPort", "tcpFlags", "packetSize", "ipSize", "samplingRate")

  def parse(serializedSflow: Array[Byte])= {
    val maybeSflow = new String(serializedSflow).split(",")
    if (maybeSflow.length == fieldNames.length) {
      val sflowRec = fieldNames.zip(maybeSflow).toMap
      val srcIP = sflowRec.get("srcIP").get
      val dstIP = sflowRec.get("dstIP").get

      if (srcIP.matches(""".*?(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}).*""") &&
        dstIP.matches(""".*?(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}).*""")) {
        Some(sflowRec)
      } else None

    } else None
  }
}
