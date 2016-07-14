package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.engine.input.InputTaskManager

/**
 * Provides methods are responsible for an eviction policy of input envelope duplicates
 * Created: 14/07/2016
 *
 * @author Kseniya Mikhaleva
 */

abstract class EvictionPolicy(manager: InputTaskManager) {
  protected val uniqueEnvelopes = manager.getUniqueEnvelopes

  def checkForDuplication(key: String, value: Array[Byte]): Boolean
}
