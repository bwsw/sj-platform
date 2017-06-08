package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.crud.rest.model.config.CreateConfigurationSettingApi
import com.bwsw.sj.crud.rest.model.provider.CreateProviderApi
import com.bwsw.sj.crud.rest.model.service.CreateServiceApi
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator

class CrudRestModule extends SjModule {
  bind[JsonDeserializationErrorMessageCreator] to new JsonDeserializationErrorMessageCreator()

  bind[CreateConfigurationSettingApi] to new CreateConfigurationSettingApi
  bind[CreateProviderApi] to new CreateProviderApi
  bind[CreateServiceApi] to new CreateServiceApi
}

object CrudRestModule {
  implicit lazy val module = new CrudRestModule
  implicit lazy val injector = module.injector
}
