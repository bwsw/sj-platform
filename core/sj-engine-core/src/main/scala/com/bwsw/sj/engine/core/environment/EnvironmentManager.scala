package com.bwsw.sj.engine.core.environment

import org.slf4j.LoggerFactory

/**
 * A common class providing for user methods that can be used in a module of specific type
 * Created: 20/07/2016
 *
 * @author Kseniya Mikhaleva
 */

abstract class EnvironmentManager {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  var isCheckpointInitiated: Boolean = false

  def initiateCheckpoint() = {
    isCheckpointInitiated = true
  }
}
