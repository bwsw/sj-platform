package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.module.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

class ModuleOutput(performanceMetrics: RegularStreamingPerformanceMetrics) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
}
