package com.bwsw.sj.common

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.FileMetadataConversion
import com.bwsw.sj.common.si.model.provider.ProviderConversion
import com.bwsw.sj.common.utils.SpecificationUtils
import scaldi.Module

class SjModule extends Module {
  bind[ConnectionRepository] to new ConnectionRepository()
  bind[ProviderConversion] to new ProviderConversion()
  bind[SpecificationUtils] to new SpecificationUtils()
  bind[FileMetadataConversion] to new FileMetadataConversion()
}

object SjModule {
  implicit lazy val module = new SjModule
  implicit lazy val injector = module.injector
}
