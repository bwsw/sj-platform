package com.bwsw.sj.stub

import com.bwsw.sj.common.module.entities._
import com.bwsw.sj.common.module.{SparkStreamingExecutor, SparkStreamingValidator}
import kafka.serializer.StringDecoder
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka.KafkaUtils


object Executor extends SparkStreamingExecutor {

  def main(args: Array[String]): Unit = {
    val executorParameters = ExecutorParameters("local[*]", "app", LaunchParameters("", InstanceType("regular","0.1","stub"), "", List(), List(), 5, Map[String, Any]()))
    //deserialize args(0) to ExecutorParameters case class and use for run module

    //(!new Validator().validate(LaunchParameters("", InstanceType("regular","0.1","stub"), "", List(), List(), 5, Map[String, Any]())))
    val ssc = init(
      executorParameters.sparkMaster,
      executorParameters.appName,
      executorParameters.launchParameters.batchInterval
    )
    run(ssc)
  }

  override def run(ssc: StreamingContext): Unit = {
    val kafkaParams = Map("metadata.broker.list" -> "192.168.1.174:9092")
    val lines =
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
        ssc,
        kafkaParams,
        Set("test8"))
    lines.foreachRDD(x => {
      val pairs = x.map(_._2).flatMap(_.split(" ")).map(word => (word, 1))
      println(pairs.count())
    })

    ssc.start()
    ssc.awaitTermination()
  }
}

class Validator extends SparkStreamingValidator {
  override def validateOptions(options: Map[String, Any]): Boolean = {
    options.nonEmpty
  }
}