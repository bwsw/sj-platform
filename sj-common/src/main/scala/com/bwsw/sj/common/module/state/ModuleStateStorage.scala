package com.bwsw.sj.common.module.state

/**
 * Trait representing state of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

trait ModuleStateStorage {
  protected var variables: Map[String, Any]

  def getState(): Map[String, Any]

  def setState(newVariables: Map[String, Any])
}
