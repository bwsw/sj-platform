package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.engine.input.InputTaskManager

/**
 * Provides methods are responsible for a fix time eviction policy of input envelope duplicates.
 * In this case a specific key will be kept within fix time
 * Created: 14/07/2016
 *
 * @author Kseniya Mikhaleva
 */

class FixTimeEvictionPolicy(manager: InputTaskManager) extends EvictionPolicy(manager) {

  def checkForDuplication(key: String, value: Array[Byte]): Boolean = {
    if (!uniqueEnvelopes.containsKey(key)) {
      uniqueEnvelopes.put(key, value)
      true
    }
    else {
      false
    }
  }
}
