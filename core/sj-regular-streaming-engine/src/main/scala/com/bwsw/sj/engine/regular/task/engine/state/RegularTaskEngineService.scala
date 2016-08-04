package com.bwsw.sj.engine.regular.task.engine.state

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Class is in charge of creating a specific ModuleEnvironmentManager (and executor)
 * depending on an instance parameter 'state-management' and performing the appropriate actions related with checkpoint
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
abstract class RegularTaskEngineService(manager: RegularTaskManager, performanceMetrics: RegularStreamingPerformanceMetrics) {

  protected val logger = LoggerFactory.getLogger(this.getClass)
  /**
   * Json serializer for deserialization of envelope
   */
  protected val serializer = new JsonSerializer()
  serializer.setIgnoreUnknown(true)

  protected val regularInstance = manager.getInstance.asInstanceOf[RegularInstance]
  protected val outputProducers = manager.outputProducers
  val moduleTimer = new SjTimer()
  val outputTags = manager.outputTags

  val moduleEnvironmentManager: ModuleEnvironmentManager
  val executor: RegularStreamingExecutor

  /**
   * Does group checkpoint of t-streams state consumers/producers
   * if it's not a stateless regular module
   */
  def doCheckpoint(): Unit = {}


}
