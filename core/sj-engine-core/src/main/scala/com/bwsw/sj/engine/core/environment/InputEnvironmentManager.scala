package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.dal.model.stream.StreamDomain

/**
 * Provides for user methods that can be used in an input module
 *
 * @author Kseniya Mikhaleva
 * @param options User defined options from instance parameters
 * @param outputs Set of output streams of instance parameters
 */
class InputEnvironmentManager(options: String, outputs: Array[StreamDomain]) extends EnvironmentManager(options, outputs) {

}
