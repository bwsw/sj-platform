package com.bwsw.sj.common


import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.FileBuffer
import com.bwsw.sj.common.si.model.FileMetadataConversion
import com.bwsw.sj.common.si.model.config.ConfigurationSettingConversion
import com.bwsw.sj.common.si.model.module.ModuleMetadataConversion
import com.bwsw.sj.common.si.model.provider.ProviderConversion
import com.bwsw.sj.common.utils.SpecificationUtils
import com.bwsw.sj.common.si.model.service.ServiceConversion
import com.bwsw.sj.common.si.model.stream.StreamConversion
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Module

class SjModule extends Module {
  bind[ConnectionRepository] to new ConnectionRepository()
  bind[MessageResourceUtils] to new MessageResourceUtils()
  bind[SpecificationUtils] to new SpecificationUtils()
  bind[SettingsUtils] to new SettingsUtils()

  bind[ProviderConversion] to new ProviderConversion()
  bind[ServiceConversion] to new ServiceConversion()
  bind[StreamConversion] to new StreamConversion()
  bind[FileMetadataConversion] to new FileMetadataConversion()
  bind[ModuleMetadataConversion] to new ModuleMetadataConversion()
  bind[ConfigurationSettingConversion] to new ConfigurationSettingConversion()

  bind[FileBuffer] toProvider new FileBuffer()
  bind[JsonSerializer] toProvider new JsonSerializer(ignore = true)
}

object SjModule {
  implicit lazy val module = new SjModule
  implicit lazy val injector = module.injector
}
