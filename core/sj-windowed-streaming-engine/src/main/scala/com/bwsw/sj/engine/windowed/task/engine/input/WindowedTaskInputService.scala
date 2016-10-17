package com.bwsw.sj.engine.windowed.task.engine.input

import java.util.concurrent.Callable

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 *
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 */
abstract class WindowedTaskInputService(manager: WindowedTaskManager) extends Callable[Unit] {

  protected val regularInstance = manager.instance.asInstanceOf[RegularInstance]
  protected val envelopeSerializer = new JsonSerializer()
  protected val objectSerializer = new ObjectSerializer()

  def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics)

  def doCheckpoint() = {}
}

