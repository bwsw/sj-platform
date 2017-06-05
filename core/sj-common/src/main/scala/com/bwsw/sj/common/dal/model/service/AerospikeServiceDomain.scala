package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.utils.ServiceLiterals

class AerospikeServiceDomain(override val name: String,
                             override val description: String,
                             @ReferenceField override val provider: ProviderDomain,
                             val namespace: String)
  extends ServiceDomain(name, description, provider, ServiceLiterals.aerospikeType) {
}
