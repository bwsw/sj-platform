package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.engine.input.task.InputTaskManager

/**
 * Provides methods are responsible for an eviction policy of input envelope duplicates
 * Created: 14/07/2016
 *
 * @author Kseniya Mikhaleva
 */

abstract class EvictionPolicy(manager: InputTaskManager) {
  protected val uniqueEnvelopes = manager.getUniqueEnvelopes

  /**
   * Checks whether a specific key is duplicate or not
   * @param key Key that will be checked
   * @param value In case there has to update duplicate key this value will be used
   * @return True if the key is not duplicate and false in other case
   */
  def checkForDuplication(key: String, value: Array[Byte]): Boolean
}
