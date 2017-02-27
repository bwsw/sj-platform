package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.engine.DefaultEnvelopeDataSerializer
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import org.slf4j.LoggerFactory

class ModuleOutput(performanceMetrics: PerformanceMetrics) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val objectSerializer = new DefaultEnvelopeDataSerializer()
}
