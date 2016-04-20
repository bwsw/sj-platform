package com.bwsw.sj.common.module.state

import scala.collection.mutable

/**
 * Сlass representing state of module
 * Created: 19/04/2016
 * @author Kseniya Mikhaleva
 */

class ModuleState(stateVariables: mutable.Map[String, Any], stateChanges: mutable.Map[String, (String, Any)] ) extends State {
  
  override def get(key: String): Any = {
    stateVariables(key)
  }

  override def set(key: String, value: Any): Unit = {
    stateChanges(key) = ("set", value)
    stateVariables(key) = value
  }

  override def delete(key: String): Unit = {
    stateChanges(key) = ("delete", stateVariables(key))
    stateVariables.remove(key)
  }

}
