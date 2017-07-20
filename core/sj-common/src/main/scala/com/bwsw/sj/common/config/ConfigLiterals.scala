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

import com.bwsw.sj.common.rest.Type

object ConfigLiterals {
  final val systemDomain = "configuration.system"
  final val tstreamsDomain = "configuration.t-streams"
  final val kafkaDomain = "configuration.apache-kafka"
  final val elasticsearchDomain = "configuration.elasticsearch"
  final val zookeeperDomain = "configuration.apache-zookeeper"
  final val jdbcDomain = "configuration.sql-database"
  val domains = Seq(systemDomain, tstreamsDomain, kafkaDomain, elasticsearchDomain, zookeeperDomain, jdbcDomain)

  val domainTypes: Seq[Type] = Map(
    systemDomain -> "System",
    tstreamsDomain -> "T-streams",
    kafkaDomain -> "Apache Kafka",
    jdbcDomain -> "SQL database",
    elasticsearchDomain -> "Elasticsearch",
    zookeeperDomain -> "Apache Zookeeper"
  ).map(x => Type(x._1, x._2)).toSeq

  val hostOfCrudRestTag = s"$systemDomain.crud-rest-host"
  val portOfCrudRestTag = s"$systemDomain.crud-rest-port"
  val marathonTag = s"$systemDomain.marathon-connect"
  val marathonTimeoutTag = s"$systemDomain.marathon-connect-timeout"
  val zkSessionTimeoutTag = s"$zookeeperDomain.session-timeout"
  val lowWatermark = s"$systemDomain.low-watermark"

  private val jdbcDriver = s"$jdbcDomain.driver"
  def getDriverPrefix(driverName: String) = s"$jdbcDriver.$driverName.prefix"
  def getDriverClass(driverName: String) = s"$jdbcDriver.$driverName.class"
  def getDriverFilename(driverName: String) = s"$jdbcDriver.$driverName"

  val kafkaSubscriberTimeoutTag = s"$systemDomain.subscriber-timeout"

  val frameworkTag = s"$systemDomain.current-framework"
  val frameworkPrincipalTag = s"$systemDomain.framework-principal"
  val frameworkSecretTag = s"$systemDomain.framework-secret"
  val frameworkBackoffSeconds = s"$systemDomain.framework-backoff-seconds"
  val frameworkBackoffFactor = s"$systemDomain.framework-backoff-factor"
  val frameworkMaxLaunchDelaySeconds = s"$systemDomain.framework-max-launch-delay-seconds"

  val zkSessionTimeoutDefault = 3000
}
