package com.bwsw.sj.common

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.provider.ProviderConversion
import com.bwsw.sj.common.si.model.stream.StreamConversion
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Module

class SjModule extends Module {
  bind[ConnectionRepository] to new ConnectionRepository()
  bind[MessageResourceUtils] to new MessageResourceUtils()
  bind[ProviderConversion] to new ProviderConversion()
  bind[StreamConversion] to new StreamConversion()
}

object SjModule {
  implicit lazy val module = new SjModule
  implicit lazy val injector = module.injector
}
