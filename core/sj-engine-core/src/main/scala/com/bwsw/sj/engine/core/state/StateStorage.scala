package com.bwsw.sj.engine.core.state

/**
  * Class representing storage of state of module that can be used only in a stateful module.
  * State should be used to keep some global module variables related to processing
  *
  * @param stateService service for a state management
  * @author Kseniya Mikhaleva
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
