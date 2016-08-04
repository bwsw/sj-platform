package com.bwsw.sj.engine.regular.task.engine.input

import java.util.concurrent.Callable

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.regular.task.RegularTaskManager

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * Created: 27/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 */
abstract class RegularTaskInputService(manager: RegularTaskManager) extends Callable[Unit] {

  protected val regularInstance = manager.getInstance.asInstanceOf[RegularInstance]
  protected val envelopeSerializer = new JsonSerializer()
  protected val objectSerializer = new ObjectSerializer()
  protected val configService = ConnectionRepository.getConfigService

  def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics)

  def doCheckpoint() = {}
}

