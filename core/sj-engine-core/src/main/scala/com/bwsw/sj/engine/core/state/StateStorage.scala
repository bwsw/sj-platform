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

  /**
   * Check whether a state variable with a specific key exists or not
   * @param key State variable name
   * @return True or false
   */
  def isExist(key: String): Boolean = {
    stateService.isExist(key)
  }

  /**
   * Gets a value of the state variable by the key
   * @param key State variable name
   * @return Value of the state variable
   */
  def get(key: String): Any = {
    stateService.get(key)
  }

  def getAll = {
    stateService.getAll
  }

  /**
   * Puts a value of the state variable by the key
   * @param key State variable name
   * @param value Value of the state variable
   */
  def set(key: String, value: Any): Unit = {
    stateService.set(key, value)
    stateService.setChange(key, value)
  }

  /**
   * Delete a state variable by the key
   * @param key State variable name
   */
  def delete(key: String): Unit = {
    stateService.delete(key)
    stateService.deleteChange(key)
  }

  /**
   * Removes all state variables. After this operation has completed,
   * the state will be empty.
   */
  def clear(): Unit = {
    stateService.clear()
    stateService.clearChange()
  }
}
