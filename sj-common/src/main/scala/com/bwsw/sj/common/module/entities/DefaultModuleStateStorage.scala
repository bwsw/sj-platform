package com.bwsw.sj.common.module.entities

/**
 * Сlass represents state of module that keeps in RAM
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class DefaultModuleStateStorage extends ModuleStateStorage{
  override var variables: Map[String, Any] = Map[String, Any]()
  
  override def getState() = variables
  
  override def setState(newVariables: Map[String, Any]) = variables = newVariables
}

/**
 * Trait represents state of module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

trait ModuleStateStorage {
  var variables: Map[String, Any]

  def getState(): Map[String, Any]

  def setState(newVariables: Map[String, Any])
}