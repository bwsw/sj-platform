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
                       udpDstPort: Int, tcpFlags: String, packetSize: Int, ipSize: Int, samplingRate: Int,
                       var srcAs: Int = 0, var dstAs: Int = 0) {
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