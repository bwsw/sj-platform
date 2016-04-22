package com.bwsw.sj.common.module.state

/**
 * Trait representing service to manage a state of module that has checkpoints (partial and full)
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

trait IStateService {

  /**
   * Gets a value of the state variable by key
   * @param key State variable name
   * @return Value of the state variable
   */
  def get(key: String): Any

  /**
   * Puts a value of the state variable by key
   * @param key State variable name
   * @param value Value of the state variable
   */
  def set(key: String, value: Any): Unit

  /**
   * Delete a state variable by key
   * @param key State variable name
   */
  def delete(key: String): Unit

  /**
   * Indicates that a state variable has changed
   * @param key State variable name
   * @param value Value of the state variable
   */
  def setChange(key: String, value: Any): Unit

  /**
   * Indicates that a state variable has deleted
   * @param key State variable name
   */
  def deleteChange(key: String): Unit

  /**
   * Saves a partial state changes
   */
  def checkpoint()

  /**
   * Saves a state
   */
  def fullCheckpoint()
}
