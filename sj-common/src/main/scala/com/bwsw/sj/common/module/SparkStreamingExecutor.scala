package com.bwsw.sj.common.module

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Trait that contains an execution logic of module
 * Created: 07/04/2016
 * @author Kseniya Mikhaleva
 */

trait SparkStreamingExecutor {

  def init(sparkMaster: String, appName: String, batchInterval: Int): StreamingContext = {
    val sconf: SparkConf = new SparkConf(true)
      .setMaster(sparkMaster).setAppName(appName)
    val ssc = new StreamingContext(
      sconf,
      Seconds(batchInterval))

    ssc
  }

  def run(ssc: StreamingContext): Unit
}
