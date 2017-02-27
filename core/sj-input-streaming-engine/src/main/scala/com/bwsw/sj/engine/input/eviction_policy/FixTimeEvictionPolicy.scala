package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.common.DAL.model.module.InputInstance

/**
 * Provides methods are responsible for a fix time eviction policy of input envelope duplicates.
 * In this case a specific key will be kept within fix time
 */

class FixTimeEvictionPolicy(instance: InputInstance) extends InputInstanceEvictionPolicy(instance) {

  /**
   * Checks whether a specific key is duplicate or not
   * @param key Key that will be checked
   * @param value In case there is a need to update duplicate key this value will be used
   * @return True if the key is not duplicate and false in other case
   */
  def checkForDuplication(key: String, value: Any): Boolean = {
    logger.debug(s"Check for duplicate a key: $key.")
    if (!uniqueEnvelopes.containsKey(key)) {
      logger.debug(s"The key: $key is not duplicate.")
      uniqueEnvelopes.put(key, value.toString)
      true
    }
    else {
      logger.debug(s"The key: $key is duplicate.")
      false
    }
  }
}
