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
                        @ReferenceField override val provider: JDBCProviderDomain,
                        val database: String)
  extends ServiceDomain(name, description, provider, ServiceLiterals.jdbcType) {

}
