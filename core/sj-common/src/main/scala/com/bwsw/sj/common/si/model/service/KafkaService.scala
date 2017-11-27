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
package com.bwsw.sj.common.si.model.service

import java.util.Date

import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.utils.ProviderLiterals
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class KafkaService(name: String,
                   provider: String,
                   val zkProvider: String,
                   description: String,
                   serviceType: String,
                   creationDate: String)
                  (implicit injector: Injector)
  extends Service(serviceType, name, provider, description, creationDate) {

  import messageResourceUtils.createMessage

  override def to(): KafkaServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new KafkaServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        zkProvider = providerRepository.get(this.zkProvider).get,
        creationDate = new Date())

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerRepository = connectionRepository.getProviderRepository

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider()

    // 'zkProvider' field
    Option(this.zkProvider) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "zkProvider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "zkProvider")
        }
        else {
          val zkProviderObj = providerRepository.get(x)
          zkProviderObj match {
            case None =>
              errors += createMessage("entity.error.doesnot.exist", "Zookeeper provider", x)
            case Some(zkProviderFormDB) =>
              if (zkProviderFormDB.providerType != ProviderLiterals.zookeeperType) {
                errors += createMessage("entity.error.must.one.type.other.given", "zkProvider", ProviderLiterals.zookeeperType, zkProviderFormDB.providerType)
              }
          }
        }
    }

    errors
  }
}
