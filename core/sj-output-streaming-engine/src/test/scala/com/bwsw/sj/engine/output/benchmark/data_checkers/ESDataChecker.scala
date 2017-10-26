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
import com.bwsw.sj.engine.core.testutils.checkers.Reader
import com.bwsw.sj.engine.output.benchmark.DataFactory.{esStreamName, openEsConnection, streamService}

import scala.collection.JavaConverters._

/**
  * Validates that data in Elasticsearch corresponds to data in input storage
  *
  * @author Kseniya Tomskikh
  */
object ESDataChecker extends SjOutputModuleChecker(ESReader$)

class ESDataChecker


object ESReader$ extends Reader[(Int, String)] {

  /**
    * Returns a data from Elasticsearch
    *
    * @return a data from Elasticsearch
    */
  override def get(): Seq[(Int, String)] = {
    val esStream: ESStreamDomain = streamService.get(esStreamName).get.asInstanceOf[ESStreamDomain]
    val (esClient, esService) = openEsConnection(esStream)
    val outputData = esClient.search(esService.index, esStream.name)

    outputData.getHits.map { hit =>
      val content = hit.getSource.asScala
      val value = content("value")
      val stringValue = content("string-value")
      (value.asInstanceOf[Int], stringValue.asInstanceOf[String])
    }
  }
}