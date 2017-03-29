package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.common.DAL.model.module.InputInstance
/**
 * Provides methods are responsible for an expanded time eviction policy of input envelope duplicates
 * In this case time, within which a specific key is kept, will increase if a duplicate appears
 */

class ExpandedTimeEvictionPolicy(instance: InputInstance) extends InputInstanceEvictionPolicy(instance) {
  private val stubValue = "stub"

  /**
   * Checks whether a specific key is duplicate or not and if it is update a value by the key
   * @param key Key that will be checked
   * @param value In case there is a need to update duplicate key this value will be used
   * @return True if the key is not duplicate and false in other case
   */
  def checkForDuplication(key: String, value: Any): Boolean = {
    logger.debug(s"Check for duplicate a key: $key.")
    if (!uniqueEnvelopes.containsKey(key)) {
      logger.debug(s"The key: $key is not duplicate.")
      uniqueEnvelopes.set(key, stubValue)
      true
    }
    else {
      logger.debug(s"The key: $key is duplicate so update the TTL of key.")
      uniqueEnvelopes.set(key, stubValue)
      false
    }
  }
}
