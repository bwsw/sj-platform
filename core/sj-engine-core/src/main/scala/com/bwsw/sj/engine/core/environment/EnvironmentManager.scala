package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.SjStream
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
 * A common class providing for user methods that can be used in a module of specific type
 * Created: 20/07/2016
 *
 * @author Kseniya Mikhaleva
 *
 * @param options User defined options from instance parameters
 * @param outputs Set of output streams of instance parameters that have tags
 */

abstract class EnvironmentManager(val options: Map[String, Any], outputs: Array[SjStream]) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  var isCheckpointInitiated: Boolean = false

  def initiateCheckpoint() = {
    isCheckpointInitiated = true
  }
}
