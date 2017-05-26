package com.bwsw.sj.engine.core.state

import com.bwsw.sj.common.dal.model.instance._

/**
  * Trait represents a service to manage a state of module.
  * State will be saved from time to time.
  * How often a full state will be saved depends on [[RegularInstanceDomain.stateFullCheckpoint]]/[[BatchInstanceDomain.stateFullCheckpoint]].
  * This parameter indicating a number of checkponts. Until then a partial state will be saved (difference between previous and current state)
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
    *
    * @param key   State variable name
    * @param value Value of the state variable
    */
  def setChange(key: String, value: Any): Unit

  /**
    * Indicates that a state variable has deleted
    *
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
