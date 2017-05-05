package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.Provider
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.model.service.{ArspkDBServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class AerospikeService(override val name: String,
                       override val description: String,
                       @ReferenceField val provider: Provider,
                       val namespace: String,
                       override val serviceType: String = ServiceLiterals.aerospikeType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData = {
    val protocolService = new ArspkDBServiceData()
    super.fillProtocolService(protocolService)

    protocolService.namespace = this.namespace
    protocolService.provider = this.provider.name

    protocolService
  }
}
