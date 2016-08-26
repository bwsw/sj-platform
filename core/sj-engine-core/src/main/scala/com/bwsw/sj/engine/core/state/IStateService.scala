package com.bwsw.sj.engine.core.state

import org.slf4j.LoggerFactory

/**
 * Trait representing service to manage a state of module that has checkpoints (partial and full)
 *
 * @author Kseniya Mikhaleva
 */

trait IStateService {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Check whether a state variable with specific key exists or not
   * @param key State variable name
   * @return True or false
   */
  def isExist(key: String): Boolean

  /**
   * Gets a value of the state variable by key
   * @param key State variable name
   * @return Value of the state variable
   */
  def get(key: String): Any

  /**
   * Gets all state variables
   * @return Set of all state variables with keys
   */
  def getAll: Map[String, Any]

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
   * Removes all state variables. After this operation has completed,
   * the state will be empty.
   */
  def clear(): Unit

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
   * Indicates that all state variables have deleted
   */
  def clearChange(): Unit

  /**
   * Saves a partial state changes
   */
  def savePartialState()

  /**
   * Saves a state
   */
  def saveFullState()

  /**
   * Returns the number of state variables
   */
  def getNumberOfVariables: Int
}
