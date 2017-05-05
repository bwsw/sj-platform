package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.Provider
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.model.service.{ServiceData, ZKCoordServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class ZKService(override val name: String,
                override val description: String,
                @ReferenceField val provider: Provider,
                val namespace: String,
                override val serviceType: String = ServiceLiterals.zookeeperType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData =
    new ZKCoordServiceData(name, namespace, provider.name, description)
}
