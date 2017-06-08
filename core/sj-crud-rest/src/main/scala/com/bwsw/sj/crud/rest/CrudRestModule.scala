package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.crud.rest.model.config.CreateConfigurationSettingApi
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator

class CrudRestModule extends SjModule {
  bind[JsonDeserializationErrorMessageCreator] to new JsonDeserializationErrorMessageCreator()

  bind[CreateConfigurationSettingApi] to new CreateConfigurationSettingApi
}

object CrudRestModule {
  implicit lazy val module = new CrudRestModule
  implicit lazy val injector = module.injector
}
