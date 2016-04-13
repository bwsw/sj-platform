package com.bwsw.sj.common.module.state

import scala.collection.mutable

/**
 * Сlass representing state of module that keeps in RAM
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class DefaultModuleStateStorage extends ModuleStateStorage{
  
  override def getState() = variables
  
  override def setState(newVariables: mutable.Map[String, Any]) = variables = newVariables

  override protected var variables: mutable.Map[String, Any] = mutable.Map[String, Any]()
}

