package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.dal.model.stream.SjStream
import org.slf4j.LoggerFactory

/**
 * A common class providing for user methods that can be used in a module of specific type
 *
 * @author Kseniya Mikhaleva
 *
 * @param options User defined options from instance parameters
 * @param outputs Set of output streams of instance parameters
 */

class EnvironmentManager(val options: String, val outputs: Array[SjStream]) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  var isCheckpointInitiated: Boolean = false

  def initiateCheckpoint() = {
    logger.debug("Initiate a checkpoint manually.")
    isCheckpointInitiated = true
  }

  /**
   * Returns a set of names of the output streams according to the set of tags
   */
  def getStreamsByTags(tags: Array[String]) = {
    logger.info(s"Get names of the streams that have set of tags: ${tags.mkString(",")}\n")
    outputs.filter(x => tags.forall(x.tags.contains)).map(_.name)
  }
}
