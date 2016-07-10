package com.bwsw.sj.engine.core.environment

import org.slf4j.LoggerFactory

/**
 * Provides for user methods that can be used in an input module
 * Created: 10/07/2016
 *
 * @author Kseniya Mikhaleva
 */
class InputEnvironmentManager {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  var isCheckpointInitiated: Boolean = false

  def initiateCheckpoint() = {
    isCheckpointInitiated = true
  }
}
