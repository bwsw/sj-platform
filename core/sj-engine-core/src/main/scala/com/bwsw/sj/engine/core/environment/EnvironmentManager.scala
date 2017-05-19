package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import org.slf4j.{Logger, LoggerFactory}

/**
  * A common class providing for user methods that can be used in a module of specific type
  *
  * @param options user defined options from instance [[InstanceDomain.options]]
  * @param outputs set of output streams [[StreamDomain]] from instance [[InstanceDomain.outputs]]
  * @author Kseniya Mikhaleva
  */

class EnvironmentManager(val options: String, val outputs: Array[StreamDomain]) {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  var isCheckpointInitiated: Boolean = false

  def initiateCheckpoint(): Unit = {
    logger.debug("Initiate a checkpoint manually.")
    isCheckpointInitiated = true
  }

  /**
    * Returns a set of names of the output streams according to the set of tags
    */
  def getStreamsByTags(tags: Array[String]): Array[String] = {
    logger.info(s"Get names of the streams that have set of tags: ${tags.mkString(",")}\n")
    outputs.filter(x => tags.forall(x.tags.contains)).map(_.name)
  }
}
