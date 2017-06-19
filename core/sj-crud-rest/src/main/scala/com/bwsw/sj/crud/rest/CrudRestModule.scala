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

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.crud.rest.instance.{InstanceDestroyerBuilder, InstanceStarterBuilder, InstanceStopperBuilder}
import com.bwsw.sj.crud.rest.model.config.ConfigurationSettingApiCreator
import com.bwsw.sj.crud.rest.model.instance.response.InstanceApiResponseCreator
import com.bwsw.sj.crud.rest.model.module.SpecificationApiCreator
import com.bwsw.sj.crud.rest.model.provider.ProviderApiCreator
import com.bwsw.sj.crud.rest.model.service.ServiceApiCreator
import com.bwsw.sj.crud.rest.model.stream.StreamApiCreator
import com.bwsw.sj.crud.rest.utils.{FileMetadataUtils, JsonDeserializationErrorMessageCreator}

class CrudRestModule extends SjModule {
  bind[JsonDeserializationErrorMessageCreator] to new JsonDeserializationErrorMessageCreator()
  bind[FileMetadataUtils] to new FileMetadataUtils

  bind[ConfigurationSettingApiCreator] to new ConfigurationSettingApiCreator
  bind[ProviderApiCreator] to new ProviderApiCreator
  bind[ServiceApiCreator] to new ServiceApiCreator
  bind[StreamApiCreator] to new StreamApiCreator
  bind[SpecificationApiCreator] to new SpecificationApiCreator
  bind[InstanceApiResponseCreator] to new InstanceApiResponseCreator

  bind[InstanceStarterBuilder] to new InstanceStarterBuilder
  bind[InstanceStopperBuilder] to new InstanceStopperBuilder
  bind[InstanceDestroyerBuilder] to new InstanceDestroyerBuilder
}

object CrudRestModule {
  implicit lazy val module = new CrudRestModule
  implicit lazy val injector = module.injector
}
