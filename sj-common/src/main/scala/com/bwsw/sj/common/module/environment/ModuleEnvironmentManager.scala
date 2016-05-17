package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.state.StateStorage
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.tstreams.agents.producer.BasicProducer

import scala.collection._

/**
 * Provides for user methods that can be used in a module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param options User defined options from instance parameters
 * @param producers T-streams producers for each output stream of instance parameters
 * @param outputTags Keeps a tag (partitioned or round-robin output) corresponding to the output for each output stream
 * @param moduleTimer Provides a possibility to set a timer inside a module
 */
class ModuleEnvironmentManager(val options: Map[String, Any],
                               producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]],
                               outputTags: mutable.Map[String, (String, Any)],
                               moduleTimer: SjTimer) {

  /**
   * Allows getting partitioned output for specific output stream
   * @param streamName Name of output stream
   * @return Partitioned output that wrapping output stream
   */
  def getPartitionedOutput(streamName: String) = {
    if (outputTags.contains(streamName)) {
      if (outputTags(streamName)._1 == "partitioned") {
        outputTags(streamName)._2.asInstanceOf[PartitionedOutput]
      }
      else {
        throw new Exception("For this output stream is set partitioned output")
      }
    } else {
      outputTags(streamName) = ("partitioned", new PartitionedOutput(producers(streamName)))

      outputTags(streamName)._2.asInstanceOf[PartitionedOutput]
    }
  }

  /**
   * Allows getting round-robin output for specific output stream
   * @param streamName Name of output stream
   * @return Round-robin output that wrapping output stream
   */
  def getRoundRobinOutput(streamName: String) = {
    if (outputTags.contains(streamName)) {
      if (outputTags(streamName)._1 == "round-robin") {
        outputTags(streamName)._2.asInstanceOf[RoundRobinOutput]
      }
      else {
        throw new Exception("For this output stream is set round-robin output")
      }
    } else {
      outputTags(streamName) = ("round-robin", new RoundRobinOutput(producers(streamName)))

      outputTags(streamName)._2.asInstanceOf[RoundRobinOutput]
    }
  }

  /**
   * Enables user to use a timer in a module which will invoke the time handler: onTimer
   * @param delay Time after which the handler will call
   */
  def setTimer(delay: Long) = moduleTimer.set(delay)

  /**
   * Provides a default method for getting state of module. Must be overridden in stateful module
   * @return Module state
   */
  def getState: StateStorage = throw new Exception("Module has no state")
}



