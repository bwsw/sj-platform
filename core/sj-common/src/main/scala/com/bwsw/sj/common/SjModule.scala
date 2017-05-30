package com.bwsw.sj.common

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import scaldi.Module

class SjModule extends Module {
  bind[ConnectionRepository] to new ConnectionRepository()
}

object SjModule {
  implicit lazy val module = new SjModule
  implicit lazy val injector = module.injector
}
