package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.engine.DefaultEnvelopeDataSerializer
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import org.slf4j.{Logger, LoggerFactory}
import com.bwsw.sj.engine.core.entities._

/**
  * Common class that is a wrapper for output stream
  *
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  * @param classLoader        it is needed for loading some custom classes from module jar to serialize/deserialize envelope data
  *                           (ref. [[TStreamEnvelope.data]] or [[KafkaEnvelope.data]])
  */
class ModuleOutput(performanceMetrics: PerformanceMetrics, classLoader: ClassLoader) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val objectSerializer: DefaultEnvelopeDataSerializer = new DefaultEnvelopeDataSerializer(classLoader)
}
