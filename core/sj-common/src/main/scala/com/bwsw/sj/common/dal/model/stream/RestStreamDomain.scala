package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.RestServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

/**
  * Stream for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestStreamDomain(override val name: String,
                       override val service: RestServiceDomain,
                       override val description: String = RestLiterals.defaultDescription,
                       override val force: Boolean = false,
                       override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.restOutputType)
