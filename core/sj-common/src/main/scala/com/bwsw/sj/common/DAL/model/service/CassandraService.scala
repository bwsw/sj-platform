package com.bwsw.sj.common.DAL.model.service

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.entities.service.{CassDBServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

class CassandraService(override val name: String,
                       override val description: String,
                       @ReferenceField val provider: Provider,
                       val keyspace: String,
                       override val serviceType: String = ServiceLiterals.cassandraType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData =
    new CassDBServiceData(name, provider.name, keyspace, description)
}
