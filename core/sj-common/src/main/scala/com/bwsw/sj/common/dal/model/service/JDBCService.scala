package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.JDBCProvider
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.rest.model.service.{JDBCServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals

/**
  *
  * @author Kseniya Tomskikh
  */
class JDBCService(override val name: String,
                  override val description: String,
                  @ReferenceField val provider: JDBCProvider,
                  val database: String,
                  override val serviceType: String = ServiceLiterals.jdbcType)
  extends Service(name, description, serviceType) {

  override def asProtocolService(): ServiceData =
    new JDBCServiceData(name, provider.name, database, description)
}
