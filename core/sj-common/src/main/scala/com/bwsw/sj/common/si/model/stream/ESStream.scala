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
package com.bwsw.sj.common.si.model.stream

import java.util.Date

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.dal.model.stream.ESStreamDomain
import com.bwsw.sj.common.utils.ServiceLiterals
import scaldi.Injector

class ESStream(name: String,
               service: String,
               tags: Array[String],
               force: Boolean,
               streamType: String,
               description: String,
               creationDate: String)
              (implicit injector: Injector)
  extends SjStream(streamType, name, service, tags, force, description, creationDate) {

  override protected val serviceType = ServiceLiterals.elasticsearchType

  override def to(): ESStreamDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new ESStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[ESServiceDomain],
      description,
      force = force,
      tags = tags,
      creationDate = new Date())
  }

  override def create(): Unit =
    if (force) clearEsStream()

  override def delete(): Unit = clearEsStream()

  private def clearEsStream(): Unit = {
    val serviceDAO = connectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[ESServiceDomain]
    val hosts = service.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val maybeUsername = Option(service.provider.login)
    val maybePassword = Option(service.provider.password)

    val client = new ElasticsearchClient(hosts, maybeUsername, maybePassword)
    client.deleteDocuments(service.index, this.name)

    client.close()
  }
}
