package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.SjStream
import scala.collection.Map

/**
 * Provides for user methods that can be used in an input module
 * Created: 10/07/2016
 *
 * @author Kseniya Mikhaleva
 *
 * @param options User defined options from instance parameters
 * @param outputs Set of output streams of instance parameters that have tags
 */
class InputEnvironmentManager(options: Map[String, Any], outputs: Array[SjStream]) extends EnvironmentManager(options, outputs) {

  /**
   * Returns set of names of the output streams according to the set of tags
   *
   * @param tags Set of tags
   * @return Set of names of the streams according to the set of tags
   */
  def getStreamsByTags(tags: Array[String]) = {
    logger.info(s"Get names of the streams that have set of tags: ${tags.mkString(",")}\n")
    outputs.filter(x => tags.forall(x.tags.contains)).map(_.name)
  }
}
