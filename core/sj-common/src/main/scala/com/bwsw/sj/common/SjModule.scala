package com.bwsw.sj.common

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.FileBuffer
import com.bwsw.sj.common.si.model.CreateFileMetadata
import com.bwsw.sj.common.si.model.config.CreateConfigurationSetting
import com.bwsw.sj.common.si.model.instance.CreateInstance
import com.bwsw.sj.common.si.model.module.CreateModuleMetadata
import com.bwsw.sj.common.si.model.provider.CreateProvider
import com.bwsw.sj.common.si.model.service.CreateService
import com.bwsw.sj.common.si.model.stream.CreateStream
import com.bwsw.sj.common.utils.{FileClassLoader, MessageResourceUtils, SpecificationUtils}
import scaldi.Module

class SjModule extends Module {
  bind[MessageResourceUtils] to new MessageResourceUtils()
  bind[SpecificationUtils] to new SpecificationUtils()
  bind[SettingsUtils] to new SettingsUtils()
  bind[FileClassLoader] to new FileClassLoader()

  val mongoAuthChecker = new MongoAuthChecker(ConnectionConstants.mongoHosts, ConnectionConstants.databaseName)
  bind[ConnectionRepository] to new ConnectionRepository(mongoAuthChecker)

  bind[CreateProvider] to new CreateProvider()
  bind[CreateService] to new CreateService()
  bind[CreateStream] to new CreateStream()
  bind[CreateFileMetadata] to new CreateFileMetadata()
  bind[CreateModuleMetadata] to new CreateModuleMetadata()
  bind[CreateInstance] to new CreateInstance()
  bind[CreateConfigurationSetting] to new CreateConfigurationSetting()

  bind[FileBuffer] toProvider new FileBuffer()
  bind[JsonSerializer] toProvider new JsonSerializer(ignore = true)
}

object SjModule {
  implicit lazy val module = new SjModule
  implicit lazy val injector = module.injector
}
