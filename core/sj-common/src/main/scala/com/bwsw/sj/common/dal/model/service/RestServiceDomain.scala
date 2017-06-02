package com.bwsw.sj.common.dal.model.service

import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{PropertyField, ReferenceField}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.eclipse.jetty.http.HttpVersion

/**
  * Service for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestServiceDomain(override val name: String,
                        override val description: String,
                        @ReferenceField override val provider: ProviderDomain,
                        @PropertyField("base-path") val basePath: String,
                        @PropertyField("http-version") val httpVersion: HttpVersion,
                        val headers: java.util.Map[String, String])
  extends ServiceDomain(name, description, provider, ServiceLiterals.restType) {
}
