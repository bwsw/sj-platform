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
package com.bwsw.sj.crud.rest

import akka.stream.scaladsl.Source
import com.bwsw.sj.common.rest.{ResponseEntity, RestResponse}
import com.bwsw.sj.crud.rest.model.config.ConfigurationSettingApi
import com.bwsw.sj.crud.rest.model.instance.response.InstanceApiResponse
import com.bwsw.sj.crud.rest.model.module.SpecificationApi
import com.bwsw.sj.crud.rest.model.provider.ProviderApi
import com.bwsw.sj.crud.rest.model.service.ServiceApi
import com.bwsw.sj.crud.rest.model.stream.StreamApi

import scala.collection.mutable

case class ConnectionResponseEntity(connection: Boolean = true) extends ResponseEntity

case class TestConnectionResponseEntity(connection: Boolean, errors: String) extends ResponseEntity

case class ProviderResponseEntity(provider: ProviderApi) extends ResponseEntity

case class ProvidersResponseEntity(providers: Seq[ProviderApi] = Seq()) extends ResponseEntity

case class RelatedToProviderResponseEntity(services: Seq[String] = Seq()) extends ResponseEntity


case class ServiceResponseEntity(service: ServiceApi) extends ResponseEntity

case class ServicesResponseEntity(services: Seq[ServiceApi] = Seq()) extends ResponseEntity

case class RelatedToServiceResponseEntity(streams: Seq[String] = Seq(),
                                          instances: Seq[String] = Seq()) extends ResponseEntity


case class StreamResponseEntity(stream: StreamApi) extends ResponseEntity

case class StreamsResponseEntity(streams: Seq[StreamApi] = Seq()) extends ResponseEntity

case class RelatedToStreamResponseEntity(instances: Seq[String] = Seq()) extends ResponseEntity

case class ConfigSettingsResponseEntity(configSettings: Seq[ConfigurationSettingApi] = mutable.Buffer()) extends ResponseEntity

case class ConfigSettingResponseEntity(configSetting: ConfigurationSettingApi) extends ResponseEntity


case class ModuleJar(filename: String, source: Source[akka.util.ByteString, scala.Any]) extends RestResponse

case class ModuleInfo(moduleType: String, moduleName: String, moduleVersion: String, uploadDate: String, size: Long)

case class ModulesResponseEntity(modules: Seq[ModuleInfo] = Seq()) extends ResponseEntity

case class RelatedToModuleResponseEntity(instances: Seq[String] = Seq()) extends ResponseEntity

case class SpecificationResponseEntity(specification: SpecificationApi) extends ResponseEntity

case class ShortInstancesResponseEntity(instances: Seq[ShortInstance] = mutable.Buffer()) extends ResponseEntity

case class InstanceResponseEntity(instance: InstanceApiResponse) extends ResponseEntity

case class InstancesResponseEntity(instances: Seq[InstanceApiResponse] = Seq()) extends ResponseEntity

case class ShortInstance(name: String,
                         moduleType: String,
                         moduleName: String,
                         moduleVersion: String,
                         description: String,
                         status: String,
                         restAddress: String,
                         creationDate: String)

case class CustomJar(filename: String, source: Source[akka.util.ByteString, scala.Any]) extends RestResponse

case class CustomJarInfo(name: String, version: String, uploadDate: String, size: Long)

case class CustomJarsResponseEntity(customJars: Seq[CustomJarInfo] = Seq()) extends ResponseEntity


case class CustomFile(filename: String, source: Source[akka.util.ByteString, scala.Any]) extends RestResponse

case class CustomFileInfo(name: String, description: String, uploadDate: String, size: Long) extends ResponseEntity

case class CustomFilesResponseEntity(customFiles: Seq[CustomFileInfo] = Seq()) extends ResponseEntity
