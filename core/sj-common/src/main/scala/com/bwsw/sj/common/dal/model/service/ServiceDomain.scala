package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField, ReferenceField}
import org.mongodb.morphia.annotations.Entity

@Entity("services")
class ServiceDomain(@IdField val name: String,
                    val description: String,
                    @ReferenceField val provider: ProviderDomain,
                    @PropertyField("type") val serviceType: String)  {

}
