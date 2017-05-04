package com.bwsw.sj.engine.core.state

/**
 * Trait representing service to manage a state of module that has checkpoints (partial and full)
 *
 * @author Kseniya Mikhaleva
 */

trait IStateService {

  def isExist(key: String): Boolean

  def get(key: String): Any

  def getAll: Map[String, Any]

  def set(key: String, value: Any): Unit

  def delete(key: String): Unit

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
  def savePartialState(): Unit

  /**
   * Saves a state
   */
  def saveFullState(): Unit

  def getNumberOfVariables: Int
}
