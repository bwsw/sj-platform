package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.DTO.service.{ServiceData, ZKCoordServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class ZKService(override val name: String,
                override val description: String,
                @ReferenceField val provider: Provider,
                val namespace: String,
                override val serviceType: String = ServiceLiterals.zookeeperType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData = {
    val protocolService = new ZKCoordServiceData()
    super.fillProtocolService(protocolService)

    protocolService.namespace = this.namespace
    protocolService.provider = this.provider.name

    protocolService
  }
}
