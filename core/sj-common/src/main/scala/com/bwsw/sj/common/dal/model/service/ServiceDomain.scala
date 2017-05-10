package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField}
import org.mongodb.morphia.annotations.Entity

@Entity("services")
class ServiceDomain(@IdField val name: String,
                    val description: String,
                    @PropertyField("type") val serviceType: String)  {

}
