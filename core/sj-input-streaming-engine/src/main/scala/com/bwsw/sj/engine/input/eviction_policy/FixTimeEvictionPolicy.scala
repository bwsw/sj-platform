package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.common.dal.model.module.InputInstance

/**
 * Provides methods are responsible for a fix time eviction policy of input envelope duplicates.
 * In this case a specific key will be kept within fix time
 */

class FixTimeEvictionPolicy(instance: InputInstance) extends InputInstanceEvictionPolicy(instance) {

  /**
   * Checks whether a specific key is duplicate or not
   * @param key Key that will be checked
   * @return True if the key is not duplicate and false in other case
   */
  def checkForDuplication(key: String): Boolean = {
    logger.debug(s"Check for duplicate a key: $key.")
    if (!uniqueEnvelopes.containsKey(key)) {
      logger.debug(s"The key: $key is not duplicate.")
      uniqueEnvelopes.put(key, stubValue)
      true
    }
    else {
      logger.debug(s"The key: $key is duplicate.")
      false
    }
  }
}
