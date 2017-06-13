package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.crud.rest.model.config.CreateConfigurationSettingApi
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
}

object CrudRestModule {
  implicit lazy val module = new CrudRestModule
  implicit lazy val injector = module.injector
}
