package com.bwsw.sj.common.module.state

/**
 * Trait representing state of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */
import scala.collection._
trait ModuleStateStorage {
  protected var variables: mutable.Map[String, Any]

  def getState(): mutable.Map[String, Any]

  def setState(newVariables: mutable.Map[String, Any])
}
