package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.JDBCProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.utils.ServiceLiterals

/**
  *
  * @author Kseniya Tomskikh
  */
class JDBCServiceDomain(override val name: String,
                        override val description: String,
                        @ReferenceField val provider: JDBCProviderDomain,
                        val database: String,
                        override val serviceType: String = ServiceLiterals.jdbcType)
  extends ServiceDomain(name, description, serviceType) {

}
