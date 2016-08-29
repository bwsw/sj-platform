package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.SjStream
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
 * A common class providing for user methods that can be used in a module of specific type
 *
 * @author Kseniya Mikhaleva
 *
 * @param options User defined options from instance parameters
 * @param outputs Set of output streams of instance parameters
 */

abstract class EnvironmentManager(val options: Map[String, Any], outputs: Array[SjStream]) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  var isCheckpointInitiated: Boolean = false

  def initiateCheckpoint() = {
    isCheckpointInitiated = true
  }

  /**
   * Returns a set of names of the output streams according to the set of tags
   *
   * @param tags Set of tags
   * @return Set of names of the streams according to the set of tags
   */
  def getStreamsByTags(tags: Array[String]) = {
    logger.info(s"Get names of the streams that have set of tags: ${tags.mkString(",")}\n")
    outputs.filter(x => tags.forall(x.tags.contains)).map(_.name)
  }
}
