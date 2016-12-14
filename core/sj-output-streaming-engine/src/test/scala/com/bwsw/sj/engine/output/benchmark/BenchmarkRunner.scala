package com.bwsw.sj.engine.output.benchmark

import java.util.logging.LogManager

import com.bwsw.sj.engine.output.OutputTaskRunner

/**
  *
  *
  * @author Kseniya Tomskikh
  *
  *
  *         MONGO_HOST=176.120.25.19:27017

  *         AGENTS_HOST=localhost
  *         AGENTS_PORTS=31000,31001
  *         CASSANDRA_HOSTS=176.120.25.19:9042
  *         ZOOKEEPER_HOSTS=176.120.25.19:2181
  *         ES_HOSTS=176.120.25.19:9300
  *         JDBC_HOSTS=0.0.0.0:5432
  *         test-jdbc-bench-instance -for jdbc
  *         test-es-bench-instance   -for elasticsearch
  *         INSTANCE_NAME=test-jdbc-bench-instance
  *         TASK_NAME=test-jdbc-bench-instance-task0
  */
object BenchmarkRunner extends App {
  LogManager.getLogManager.reset()
  OutputTaskRunner.main(Array())
}
