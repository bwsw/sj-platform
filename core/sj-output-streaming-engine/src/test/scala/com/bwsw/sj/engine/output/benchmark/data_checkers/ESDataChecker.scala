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
package com.bwsw.sj.engine.output.benchmark.data_checkers

import com.bwsw.sj.common.dal.model.stream.ESStreamDomain
import com.bwsw.sj.engine.output.benchmark.DataFactory.{esStreamName, openEsConnection, streamService}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * @author Kseniya Tomskikh
  */
object ESDataChecker extends DataChecker {

  override def getOutputElements(): ArrayBuffer[(Int, String)] = {
    val esStream: ESStreamDomain = streamService.get(esStreamName).get.asInstanceOf[ESStreamDomain]
    val (esClient, esService) = openEsConnection(esStream)
    val outputData = esClient.search(esService.index, esStream.name)
    val outputElements = ArrayBuffer.empty[(Int, String)]

    outputData.getHits.foreach { hit =>
      val content = hit.getSource.asScala
      val value = content("value")
      val stringValue = content("string-value")
      outputElements.append((value.asInstanceOf[Int], stringValue.asInstanceOf[String]))
    }

    outputElements
  }
}

class ESDataChecker
