package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.tstreams.agents.producer.BasicProducer
import org.slf4j.LoggerFactory

import scala.collection._

/**
 * Provides for user methods that can be used in a module
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param options User defined options from instance parameters
 * @param producers T-streams producers for each output stream of instance parameters
 * @param outputs Set of output streams of instance parameters that have tags
 * @param outputTags Keeps a tag (partitioned or round-robin output) corresponding to the output for each output stream
 * @param moduleTimer Provides a possibility to set a timer inside a module
 */
class ModuleEnvironmentManager(val options: Map[String, Any],
                               producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]],
                               outputs: Array[SjStream],
                               outputTags: mutable.Map[String, (String, Any)],
                               moduleTimer: SjTimer) {
  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Allows getting partitioned output for specific output stream
   * @param streamName Name of output stream
   * @return Partitioned output that wrapping output stream
   */
  def getPartitionedOutput(streamName: String) = {
    logger.info(s"Get partitioned output for stream: $streamName\n")
    if (producers.contains(streamName)) {
      if (outputTags.contains(streamName)) {
        if (outputTags(streamName)._1 == "partitioned") {
          outputTags(streamName)._2.asInstanceOf[PartitionedOutput]
        }
        else {
          logger.error(s"For output stream: $streamName partitioned output is set")
          throw new Exception(s"For output stream: $streamName partitioned output is set")
        }
      } else {
        outputTags(streamName) = ("partitioned", new PartitionedOutput(producers(streamName)))

        outputTags(streamName)._2.asInstanceOf[PartitionedOutput]
      }
    } else {
      logger.error(s"There is no output for name $streamName")
      throw new IllegalArgumentException(s"There is no output for name $streamName")
    }
  }

  /**
   * Allows getting round-robin output for specific output stream
   * @param streamName Name of output stream
   * @return Round-robin output that wrapping output stream
   */
  def getRoundRobinOutput(streamName: String) = {
    logger.info(s"Get round-robin output for stream: $streamName\n")
    if (producers.contains(streamName)) {
      if (outputTags.contains(streamName)) {
        if (outputTags(streamName)._1 == "round-robin") {
          outputTags(streamName)._2.asInstanceOf[RoundRobinOutput]
        }
        else {
          logger.error(s"For output stream: $streamName partitioned output is set")
          throw new Exception(s"For output stream: $streamName partitioned output is set")
        }
      } else {
        outputTags(streamName) = ("round-robin", new RoundRobinOutput(producers(streamName)))

        outputTags(streamName)._2.asInstanceOf[RoundRobinOutput]
      }
    } else {
      logger.error(s"There is no output for name $streamName")
      throw new IllegalArgumentException(s"There is no output for name $streamName")
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
  def getState: StateStorage = {
    logger.error("Module has no state")
    throw new Exception("Module has no state")
  }

  /**
   * Returns set of names of the output streams according to the set of tags
   * @param tags Set of tags
   * @return Set of names of the streams according to the set of tags
   */
  def getStreamsByTags(tags: Array[String]) = {
    logger.info(s"Get names of the streams that have set of tags: ${tags.mkString(",")}\n")
    outputs.filter(x => tags.forall(x.tags.contains)).map(_.name)
  }
}



