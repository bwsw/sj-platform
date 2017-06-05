package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.utils.ServiceLiterals

class TStreamServiceDomain(override val name: String,
                           override val description: String,
                           @ReferenceField override val provider: ProviderDomain,
                           val prefix: String,
                           val token: String)
  extends ServiceDomain(name, description, provider, ServiceLiterals.tstreamsType) {

}
