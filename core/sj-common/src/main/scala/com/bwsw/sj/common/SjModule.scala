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
package com.bwsw.sj.common

import com.bwsw.common.JsonSerializer
import com.bwsw.common.http.HttpClientBuilder
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si._
import com.bwsw.sj.common.si.model.FileMetadataCreator
import com.bwsw.sj.common.si.model.config.ConfigurationSettingCreator
import com.bwsw.sj.common.si.model.instance.InstanceCreator
import com.bwsw.sj.common.si.model.module.ModuleMetadataCreator
import com.bwsw.sj.common.si.model.provider.ProviderCreator
import com.bwsw.sj.common.si.model.service.ServiceCreator
import com.bwsw.sj.common.si.model.stream.StreamCreator
import com.bwsw.sj.common.utils.{MessageResourceUtils, SpecificationUtils}
import scaldi.Module

class SjModule extends Module {
  bind[MessageResourceUtils] to new MessageResourceUtils
  bind[SpecificationUtils] to new SpecificationUtils
  bind[SettingsUtils] to new SettingsUtils

  val mongoAuthChecker = new MongoAuthChecker(ConnectionConstants.mongoHosts, ConnectionConstants.databaseName)
  bind[ConnectionRepository] to new ConnectionRepository(mongoAuthChecker)

  bind[ProviderCreator] to new ProviderCreator
  bind[ServiceCreator] to new ServiceCreator
  bind[StreamCreator] to new StreamCreator
  bind[FileMetadataCreator] to new FileMetadataCreator
  bind[ModuleMetadataCreator] to new ModuleMetadataCreator
  bind[InstanceCreator] to new InstanceCreator
  bind[ConfigurationSettingCreator] to new ConfigurationSettingCreator

  bind[ConfigSettingsSI] to new ConfigSettingsSI
  bind[ProviderSI] to new ProviderSI
  bind[ServiceSI] to new ServiceSI
  bind[StreamSI] to new StreamSI
  bind[CustomFilesSI] to new CustomFilesSI
  bind[CustomJarsSI] to new CustomJarsSI
  bind[ModuleSI] to new ModuleSI
  bind[InstanceSI] to new InstanceSI

  bind[FileBuffer] toProvider new FileBuffer
  bind[JsonSerializer] toProvider new JsonSerializer(ignore = true)

  bind[HttpClientBuilder] to new HttpClientBuilder
}

object SjModule {
  implicit lazy val module = new SjModule
  implicit lazy val injector = module.injector
}
