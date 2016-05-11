package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.state.StateStorage
import com.bwsw.sj.common.module.utils.SjTimer
import com.bwsw.tstreams.agents.producer.BasicProducer

import scala.collection.mutable

/**
 * Class allowing to manage environment of module that has state
 * Created: 15/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param stateStorage
 * @param options User defined options from instance parameters
 * @param producers T-streams producers for each output stream of instance parameters
 * @param outputTags Keeps a tag (partitioned or round-robin output) corresponding to the output for each output stream
 * @param moduleTimer
 */

class StatefulModuleEnvironmentManager(stateStorage: StateStorage,
                                       options: Map[String, Any],
                                       producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]],
                                       outputTags: mutable.Map[String, (String, Any)],
                                       moduleTimer: SjTimer) extends ModuleEnvironmentManager(options, producers, outputTags, moduleTimer) {
  /**
   * Returns specific state of module
   * @return Module state
   */
  override def getState: StateStorage = stateStorage
}
