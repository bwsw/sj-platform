package com.bwsw.sj.common.module.environment

import com.bwsw.sj.common.module.SjTimer
import com.bwsw.sj.common.module.state.StateStorage

import scala.collection.mutable

/**
 * Provides for user methods that can be used in a module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class ModuleEnvironmentManager(val options: Map[String, Any], //mutable.MutableList[Array[Byte]]
                               temporaryOutput: mutable.Map[String, (String, Any)],
                               moduleTimer: SjTimer) {

  /**
   * Allows getting specific output
   * @param streamName Name of output stream
   * @return Store of output that wrapping output stream
   */
  def getPartitionedOutput(streamName: String) = {
    if (temporaryOutput.contains(streamName)) {
      if (temporaryOutput(streamName)._1 == "partitioned") {
        new PartitionedOutput(temporaryOutput(streamName)._2.asInstanceOf[mutable.Map[Int, mutable.MutableList[Array[Byte]]]])
      }
      else {
        throw new Exception("For this output stream is set partitioned output")
      }
    } else {
      temporaryOutput(streamName) = ("partitioned", mutable.Map[Int, mutable.MutableList[Array[Byte]]]())

      new PartitionedOutput(temporaryOutput(streamName)._2.asInstanceOf[mutable.Map[Int, mutable.MutableList[Array[Byte]]]])
    }
  }

  def getRoundRobinOutput(streamName: String) = {
    if (temporaryOutput.contains(streamName)) {
      if (temporaryOutput(streamName)._1 == "roundrobin") {
        new RoundRobinOutput(temporaryOutput(streamName)._2.asInstanceOf[mutable.MutableList[Array[Byte]]])
      }
      else {
        throw new Exception("For this output stream is set round robin output")
      }
    } else {
      temporaryOutput(streamName) = ("roundrobin", mutable.MutableList[Array[Byte]]())

      new RoundRobinOutput(temporaryOutput(streamName)._2.asInstanceOf[mutable.MutableList[Array[Byte]]])
    }
  }
  /**
   * Enables user to use a timer in a module which will invoke the time handler: onTimer
   * @param delay Time after which the handler will call
   */
  def setTimer(delay: Long) = moduleTimer.setTimer(delay)

  /**
   * Provides a default method for getting state of module. Must be overridden in stateful module
   * @return Module state
   */
  def getState: StateStorage = throw new Exception("Module has no state")
}

class PartitionedOutput(output: mutable.Map[Int, mutable.MutableList[Array[Byte]]]) {
  def put(data: Array[Byte], partition: Int) = output(partition).+=(data)
}

class RoundRobinOutput(output: mutable.MutableList[Array[Byte]]) {
  def put(data: Array[Byte]) = output.+=(data)
}