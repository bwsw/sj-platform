package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

class ESStreamDomain(override val name: String,
                     override val service: ESServiceDomain,
                     override val description: String = RestLiterals.defaultDescription,
                     override val force: Boolean = false,
                     override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.esOutputType)
