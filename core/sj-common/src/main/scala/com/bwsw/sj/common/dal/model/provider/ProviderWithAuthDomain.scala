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
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField

import scala.collection.mutable.ArrayBuffer


/**
  * protected methods and variables need for testing purposes
  */
class ProviderWithAuthDomain(override val name: String,
                             override val description: String,
                             override val hosts: Array[String],
                             val login: String,
                             val password: String,
                             @PropertyField("provider-type") override val providerType: String,
                             override val creationDate: Date)
  extends ProviderDomain(name, description, hosts, providerType, creationDate) {

  override protected def checkESConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val client = new ElasticsearchClient(Set(getHostAndPort(address)), Option(login), Option(password))
    if (!client.isConnected()) {
      errors += s"Can not establish connection to ElasticSearch on '$address'"
    }
    client.close()

    errors
  }
}
