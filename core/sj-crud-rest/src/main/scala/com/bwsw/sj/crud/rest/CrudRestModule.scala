package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.crud.rest.instance.{InstanceDestroyerBuilder, InstanceStarterBuilder, InstanceStopperBuilder}
import com.bwsw.sj.crud.rest.model.config.CreateConfigurationSettingApi
import com.bwsw.sj.crud.rest.model.instance.response.CreateInstanceApiResponse
import com.bwsw.sj.crud.rest.model.module.CreateSpecificationApi
import com.bwsw.sj.crud.rest.model.provider.CreateProviderApi
import com.bwsw.sj.crud.rest.model.service.CreateServiceApi
import com.bwsw.sj.crud.rest.model.stream.CreateStreamApi
import com.bwsw.sj.crud.rest.utils.{FileMetadataUtils, JsonDeserializationErrorMessageCreator}

class CrudRestModule extends SjModule {
  bind[JsonDeserializationErrorMessageCreator] to new JsonDeserializationErrorMessageCreator()
  bind[FileMetadataUtils] to new FileMetadataUtils

  bind[CreateConfigurationSettingApi] to new CreateConfigurationSettingApi
  bind[CreateProviderApi] to new CreateProviderApi
  bind[CreateServiceApi] to new CreateServiceApi
  bind[CreateStreamApi] to new CreateStreamApi
  bind[CreateSpecificationApi] to new CreateSpecificationApi
  bind[CreateInstanceApiResponse] to new CreateInstanceApiResponse

  bind[InstanceStarterBuilder] to new InstanceStarterBuilder
  bind[InstanceStopperBuilder] to new InstanceStopperBuilder
  bind[InstanceDestroyerBuilder] to new InstanceDestroyerBuilder
}

object CrudRestModule {
  implicit lazy val module = new CrudRestModule
  implicit lazy val injector = module.injector
}
