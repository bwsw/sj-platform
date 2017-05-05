package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.entities.service.{EsServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class ESService(override val name: String,
                override val description: String,
                @ReferenceField val provider: Provider,
                val index: String,
                override val serviceType: String = ServiceLiterals.elasticsearchType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData = {
    val protocolService = new EsServiceData()
    super.fillProtocolService(protocolService)

    protocolService.index = this.index
    protocolService.provider = this.provider.name

    protocolService
  }
}
