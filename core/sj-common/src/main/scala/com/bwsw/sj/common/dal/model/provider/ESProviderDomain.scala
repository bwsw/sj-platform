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
package com.bwsw.sj.common.dal.model.provider

import java.util.Date

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.utils.ProviderLiterals

import scala.collection.mutable.ArrayBuffer


/**
  * protected methods and variables need for testing purposes
  */
class ESProviderDomain(name: String,
                       description: String,
                       hosts: Array[String],
                       val login: String,
                       val password: String,
                       creationDate: Date)
  extends ProviderDomain(name, description, hosts, ProviderLiterals.elasticsearchType, creationDate) {
  import ProviderDomain._

  override protected def checkESConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val client = new ElasticsearchClient(Set(getHostAndPort(address)), Option(login), Option(password))
    if (!client.isConnected()) {
      errors += messageResourceUtils.createMessage("rest.providers.provider.cannot.connect.es", address)
    }
    client.close()

    errors
  }
}
