package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.utils.ServiceLiterals

class AerospikeServiceDomain(override val name: String,
                             override val description: String,
                             @ReferenceField val provider: ProviderDomain,
                             val namespace: String,
                             override val serviceType: String = ServiceLiterals.aerospikeType)
  extends ServiceDomain(name, description, serviceType) {
}
