package com.bwsw.sj.common.engine

trait StateHandlers {

  /**
   * Handler triggered before saving of the state
   *
   * @param isFullState Flag denotes that the full state (true) or partial changes of state (false) will be saved
   */
  def onBeforeStateSave(isFullState: Boolean): Unit = {}

  /**
   * Handler triggered after saving of the state
   *
   * @param isFullState Flag denotes that there was(were) a saving of the full state (true) or partial changes of state(false)
   */
  def onAfterStateSave(isFullState: Boolean): Unit = {}
}
