package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.SjStream
import scala.collection.Map

/**
 * Provides for user methods that can be used in an input module
 *
 *
 * @author Kseniya Mikhaleva
 *
 * @param options User defined options from instance parameters
 * @param outputs Set of output streams of instance parameters that have tags
 */
class InputEnvironmentManager(options: Map[String, Any], outputs: Array[SjStream]) extends EnvironmentManager(options, outputs) {

}
