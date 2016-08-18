package com.bwsw.sj.engine.input.eviction_policy

import com.bwsw.sj.common.DAL.model.module.InputInstance

/**
 * Provides methods are responsible for an expanded time eviction policy of input envelope duplicates
 * In this case time, within which a specific key is kept, will increase if a duplicate appears
 * Created: 14/07/2016
 *
 * @param instance Input instance contains a settings of an eviction policy
 *                 (message TTL, a default eviction policy and a maximum size of message queue)
 * @author Kseniya Mikhaleva
 */

class ExpandedTimeEvictionPolicy(instance: InputInstance) extends InputInstanceEvictionPolicy(instance) {

  /**
   * Checks whether a specific key is duplicate or not and if it is update a value by the key
   * @param key Key that will be checked
   * @param value In case there has to update duplicate key this value will be used
   * @return True if the key is not duplicate and false in other case
   */
  def checkForDuplication(key: String, value: Array[Byte]): Boolean = {
    logger.debug(s"Check for duplicate key: $key")
    if (!uniqueEnvelopes.containsKey(key)) {
      logger.debug(s"The key: $key is not duplicate")
      uniqueEnvelopes.put(key, value)
      true
    }
    else {
      logger.debug(s"The key: $key is duplicate so update the TTL of key")
      uniqueEnvelopes.replace(key, value)
      false
    }
  }
}
