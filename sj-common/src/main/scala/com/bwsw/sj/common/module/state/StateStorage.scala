package com.bwsw.sj.common.module.state

/**
 * Сlass representing storage of state of module.
 * State may be used to keeping some global module variables related to processing
 * Created: 19/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param stateService Service for state management
 */

class StateStorage(stateService: IStateService) {

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
    stateService.setChange(key, value)
    stateService.set(key, value)
  }

  /**
   * Delete a state variable by key
   * @param key State variable name
   */
  def delete(key: String): Unit = {
    stateService.deleteChange(key)
    stateService.delete(key)
  }

  /**
   * Removes all state variables. After this operation has completed,
   *  the state will be empty.
   */
  def clear(): Unit = {
    stateService.clearChange()
    stateService.clear()
  }

}
