package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.Callable

import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 *
 * @author Kseniya Mikhaleva
 */
trait TaskInputService extends Callable[Unit] {

  def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics)

  def doCheckpoint()
}

