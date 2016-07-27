package com.bwsw.sj.engine.regular.task.engine.input

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
abstract class RegularTaskInputService(manager: RegularTaskManager) extends Runnable {

  protected val regularInstance = manager.getInstanceMetadata.asInstanceOf[RegularInstance]
  protected val serializer = new JsonSerializer()
  protected val objectSerializer = new ObjectSerializer()
  protected val configService = ConnectionRepository.getConfigService

  def processEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics)

  def doCheckpoint() = {}
}

