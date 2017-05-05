package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.entities.service.{ServiceData, TstrQServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class TStreamService(override val name: String,
                     override val description: String,
                     @ReferenceField val provider: Provider,
                     val prefix: String,
                     val token: String,
                     override val serviceType: String = ServiceLiterals.tstreamsType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData = {
    val protocolService = new TstrQServiceData()
    super.fillProtocolService(protocolService)

    protocolService.provider = this.provider.name
    protocolService.prefix = this.prefix
    protocolService.token = this.token

    protocolService
  }
}
