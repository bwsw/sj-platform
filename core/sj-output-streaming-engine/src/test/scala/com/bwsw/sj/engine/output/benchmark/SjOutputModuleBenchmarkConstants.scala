/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.output.benchmark

object SjOutputModuleBenchmarkConstants {
  val countTxns = 20
  val countElements = 50
  val totalInputElements = countTxns * countElements
  val checkpointInterval = 5
  val databaseName = "test_database_for_output_engine"
  val jdbcUsername = "admin"
  val jdbcPassword = "admin"

  val esInstanceName = "test-es-instance-for-output-engine"
  val jdbcInstanceName = "test-jdbc-instance-for-output-engine"
  val restInstanceName = "test-rest-instance-for-output-engine"

  val pathToESModule = "../../contrib/stubs/sj-stub-es-output-streaming/target/scala-2.12/sj-stub-es-output-streaming-1.0-SNAPSHOT.jar"
  val pathToJdbcModule = "../../contrib/stubs/sj-stub-jdbc-output-streaming/target/scala-2.12/sj-stub-jdbc-output-streaming-1.0-SNAPSHOT.jar"
  val pathToRestModule = "../../contrib/stubs/sj-stub-rest-output-streaming/target/scala-2.12/sj-stub-rest-output-streaming-1.0-SNAPSHOT.jar"
}
