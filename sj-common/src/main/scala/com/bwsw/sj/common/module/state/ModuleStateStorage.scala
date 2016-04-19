package com.bwsw.sj.common.module.state

/**
 * Trait representing state of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

import scala.collection._

trait ModuleStateStorage {
  protected var stateVariables: mutable.Map[String, Any]

  protected val stateChanges: mutable.Map[String, (String, Any)]

  def getState(): mutable.Map[String, Any]

  def checkpoint()

  def fullCheckpoint()
}
