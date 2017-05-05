package com.bwsw.sj.engine.core.state

/**
 * Class representing storage of state of module that can be used only in a stateful module.
 * State can be used to keeping some global module variables related to processing
 *
 * @author Kseniya Mikhaleva
 *
 * @param stateService Service for a state management
 */

class StateStorage(stateService: IStateService) {

  def isExist(key: String): Boolean = {
    stateService.isExist(key)
  }

  def get(key: String): Any = {
    stateService.get(key)
  }

  def getAll: Map[String, Any] = {
    stateService.getAll
  }

  def set(key: String, value: Any): Unit = {
    stateService.set(key, value)
    stateService.setChange(key, value)
  }

  def delete(key: String): Unit = {
    stateService.delete(key)
    stateService.deleteChange(key)
  }

  def clear(): Unit = {
    stateService.clear()
    stateService.clearChange()
  }
}
