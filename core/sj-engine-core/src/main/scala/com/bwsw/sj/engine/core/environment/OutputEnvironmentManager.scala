package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.stream.SjStream

/**
 * Provides for user methods that can be used in an output module
 *
 * @author Kseniya Mikhaleva
 * @param options User defined options from instance parameters
 * @param outputs Set of output streams of instance parameters
 */
class OutputEnvironmentManager(options: String, outputs: Array[SjStream]) extends EnvironmentManager(options, outputs) {

}