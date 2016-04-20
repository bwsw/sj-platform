package com.bwsw.sj.common.module.state

/**
 * Trait representing storage for state of module that has checkpoints (partial and full)
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

trait IStateService {

  def getState: StateStorage

  def get(key: String): Any

  def set(key: String, value: Any): Unit

  def delete(key: String): Unit

  def setChange(key: String, value: Any): Unit

  def deleteChange(key: String): Unit

  def checkpoint()

  def fullCheckpoint()
}
