package com.bwsw.sj.common.utils.stream_distributor

import com.bwsw.sj.common.utils.EngineLiterals

/**
  * Policy indicating how envelopes should be distributed by partitions.
  * Used in [[EngineLiterals.inputStreamingType]] engine
  */

trait StreamDistributionPolicy

case object RoundRobin extends StreamDistributionPolicy

case object ByHash extends StreamDistributionPolicy
