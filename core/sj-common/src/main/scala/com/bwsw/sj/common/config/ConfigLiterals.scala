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
package com.bwsw.sj.common.config

object ConfigLiterals {
  final val systemDomain = "system"
  final val tstreamsDomain = "t-streams"
  final val kafkaDomain = "kafka"
  final val elasticsearchDomain = "es"
  final val zookeeperDomain = "zk"
  final val jdbcDomain = "jdbc"
  final val restDomain = "rest"
  val domains = Seq(systemDomain, tstreamsDomain, kafkaDomain, elasticsearchDomain, zookeeperDomain, jdbcDomain, restDomain)
  val hostOfCrudRestTag = s"$systemDomain.crud-rest-host"
  val portOfCrudRestTag = s"$systemDomain.crud-rest-port"
  val marathonTag = s"$systemDomain.marathon-connect"
  val marathonTimeoutTag = s"$systemDomain.marathon-connect-timeout"
  val zkSessionTimeoutTag = s"$zookeeperDomain.session-timeout"
  val lowWatermark = s"$systemDomain.low-watermark"

  val jdbcDriver = s"$jdbcDomain.driver"

  val kafkaSubscriberTimeoutTag = s"$systemDomain.subscriber-timeout"

  val frameworkTag = s"$systemDomain.current-framework"
  val frameworkPrincipalTag = s"$systemDomain.framework-principal"
  val frameworkSecretTag = s"$systemDomain.framework-secret"
  val frameworkBackoffSeconds = s"$systemDomain.framework-backoff-seconds"
  val frameworkBackoffFactor = s"$systemDomain.framework-backoff-factor"
  val frameworkMaxLaunchDelaySeconds = s"$systemDomain.framework-max-launch-delay-seconds"

  val zkSessionTimeoutDefault = 3000
}
