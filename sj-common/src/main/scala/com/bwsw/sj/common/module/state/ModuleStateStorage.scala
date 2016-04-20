package com.bwsw.sj.common.module.state

import scala.collection.mutable

/**
 * Trait representing storage for state of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

trait ModuleStateStorage {

  protected val stateVariables: mutable.Map[String, Any]
  protected val stateChanges: mutable.Map[String, (String, Any)]

  def getState: State

  def checkpoint()

  def fullCheckpoint()
}
