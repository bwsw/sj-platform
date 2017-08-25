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

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.dal.model.stream.RestStreamDomain
import com.bwsw.sj.engine.output.benchmark.DataFactory.{restStreamName, streamService}
import com.bwsw.sj.engine.output.benchmark.OutputTestRestServer.Entity
import org.eclipse.jetty.client.HttpClient

import scala.util.Try

object RestDataChecker extends DataChecker {

  override def getOutputElements(): Seq[(Int, String)] = {
    val restStream: RestStreamDomain = streamService.get(restStreamName).get.asInstanceOf[RestStreamDomain]

    val jsonSerializer = new JsonSerializer()

    val hosts = restStream.service.asInstanceOf[RestServiceDomain].provider.hosts
    val urls = hosts.map("http://" + _)
    var outputElements = Seq.empty[(Int, String)]
    val client = new HttpClient()
    client.start()
    urls.exists(url => Try {
      val response = client.GET(url)
      val data = response.getContentAsString
      val list = jsonSerializer.deserialize[Iterable[Entity]](data)
      outputElements = list.map(e => (e.value, e.stringValue)).toSeq

      outputElements.nonEmpty
    }.getOrElse(false))
    client.stop()

    outputElements
  }
}

class RestDataChecker
