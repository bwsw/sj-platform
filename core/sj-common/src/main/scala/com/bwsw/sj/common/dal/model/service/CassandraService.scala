package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.Provider
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.model.service.{CassDBServiceData, ServiceData}
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
