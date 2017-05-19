package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain

/**
  * Provides for user methods that can be used in an output module
  *
  * @param options user defined options from instance [[InstanceDomain.options]]
  * @param outputs set of output streams [[StreamDomain]] from instance [[InstanceDomain.outputs]]
  * @author Kseniya Mikhaleva
  */
class OutputEnvironmentManager(options: String, outputs: Array[StreamDomain]) extends EnvironmentManager(options, outputs) {

}