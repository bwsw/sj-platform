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
package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service._
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}
import scaldi.Injector

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ServiceApi], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[EsServiceApi], name = ServiceLiterals.elasticsearchType),
  new Type(value = classOf[KfkQServiceApi], name = ServiceLiterals.kafkaType),
  new Type(value = classOf[TstrQServiceApi], name = ServiceLiterals.tstreamsType),
  new Type(value = classOf[ZKCoordServiceApi], name = ServiceLiterals.zookeeperType),
  new Type(value = classOf[JDBCServiceApi], name = ServiceLiterals.jdbcType),
  new Type(value = classOf[RestServiceApi], name = ServiceLiterals.restType)
))
class ServiceApi(@JsonProperty("type") val serviceType: String,
                 val name: String,
                 val provider: String,
                 val description: Option[String] = Some(RestLiterals.defaultDescription),
                 val creationDate: String) {

  @JsonIgnore
  def to()(implicit injector: Injector): Service =
    new Service(
      serviceType = this.serviceType,
      name = this.name,
      provider = this.provider,
      description = this.description.getOrElse(RestLiterals.defaultDescription),
      creationDate = this.creationDate
    )
}

class ServiceApiCreator {

  def from(service: Service): ServiceApi = {
    service.serviceType match {
      case ServiceLiterals.elasticsearchType =>
        val esService = service.asInstanceOf[ESService]

        new EsServiceApi(
          name = esService.name,
          index = esService.index,
          provider = esService.provider,
          description = Option(esService.description),
          creationDate = esService.creationDate
        )

      case ServiceLiterals.jdbcType =>
        val jdbcService = service.asInstanceOf[JDBCService]

        new JDBCServiceApi(
          name = jdbcService.name,
          database = jdbcService.database,
          provider = jdbcService.provider,
          description = Option(jdbcService.description),
          creationDate = jdbcService.creationDate
        )

      case ServiceLiterals.kafkaType =>
        val kafkaService = service.asInstanceOf[KafkaService]

        new KfkQServiceApi(
          name = kafkaService.name,
          zkProvider = kafkaService.zkProvider,
          zkNamespace = kafkaService.zkNamespace,
          provider = kafkaService.provider,
          description = Option(kafkaService.description),
          creationDate = kafkaService.creationDate
        )

      case ServiceLiterals.restType =>
        val restService = service.asInstanceOf[RestService]

        new RestServiceApi(
          name = restService.name,
          basePath = Option(restService.basePath),
          httpVersion = Option(restService.httpVersion),
          headers = Option(restService.headers),
          provider = restService.provider,
          description = Option(restService.description),
          creationDate = restService.creationDate
        )

      case ServiceLiterals.tstreamsType =>
        val tStreamService = service.asInstanceOf[TStreamService]

        new TstrQServiceApi(
          name = tStreamService.name,
          prefix = tStreamService.prefix,
          token = tStreamService.token,
          provider = tStreamService.provider,
          description = Option(tStreamService.description),
          creationDate = tStreamService.creationDate
        )

      case ServiceLiterals.zookeeperType =>
        val zkService = service.asInstanceOf[ZKService]

        new ZKCoordServiceApi(
          name = zkService.name,
          namespace = zkService.namespace,
          provider = zkService.provider,
          description = Option(zkService.description),
          creationDate = zkService.creationDate
        )
    }
  }
}
