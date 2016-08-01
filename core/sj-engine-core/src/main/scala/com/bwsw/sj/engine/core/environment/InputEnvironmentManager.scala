package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.SjStream

/**
 * Provides for user methods that can be used in an input module
 * Created: 10/07/2016
 *
 * @author Kseniya Mikhaleva
 */
class InputEnvironmentManager(outputs: Array[SjStream]) extends EnvironmentManager {

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
