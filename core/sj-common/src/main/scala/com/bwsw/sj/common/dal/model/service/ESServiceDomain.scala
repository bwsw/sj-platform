package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.ReferenceField
import com.bwsw.sj.common.utils.ServiceLiterals

class ESServiceDomain(override val name: String,
                      override val description: String,
                      @ReferenceField val provider: ProviderDomain,
                      val index: String)
  extends ServiceDomain(name, description, ServiceLiterals.elasticsearchType) {
}
