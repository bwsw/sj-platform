package com.bwsw.sj.engine.core.state

/**
 * Сlass representing storage of state of module.
 * State can be used to keeping some global module variables related to processing
 * Created: 19/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param stateService Service for state management
 */

class StateStorage(stateService: IStateService) {

  /**
   * Check whether a state variable with specific key exists or not
   * @param key State variable name
   * @return True or false
   */
  def isExist(key: String): Boolean = {
    stateService.isExist(key)
  }

  /**
   * Gets a value of the state variable by key
   * @param key State variable name
   * @return Value of the state variable
   */
  def get(key: String): Any = {
    stateService.get(key)
  }

  /**
   * Puts a value of the state variable by key
   * @param key State variable name
   * @param value Value of the state variable
   */
  def set(key: String, value: Any): Unit = {
    stateService.set(key, value)
    stateService.setChange(key, value)
  }

  /**
   * Delete a state variable by key
   * @param key State variable name
   */
  def delete(key: String): Unit = {
    stateService.delete(key)
    stateService.deleteChange(key)
  }

  /**
   * Removes all state variables. After this operation has completed,
   *  the state will be empty.
   */
  def clear(): Unit = {
    stateService.clear()
    stateService.clearChange()
  }

}
