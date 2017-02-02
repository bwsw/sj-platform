package com.bwsw.sj.common.config

object ConfigLiterals {
  final val systemDomain = "system"
  final val tstreamsDomain = "t-streams"
  final val kafkaDomain = "kafka"
  final val elasticsearchDomain = "es"
  final val zookeeperDomain = "zk"
  final val jdbcDomain = "jdbc"
  val domains = Seq(systemDomain, tstreamsDomain, kafkaDomain, elasticsearchDomain, zookeeperDomain, jdbcDomain)
  val hostOfCrudRestTag = s"$systemDomain.crud-rest-host"
  val portOfCrudRestTag = s"$systemDomain.crud-rest-port"
  val marathonTag = s"$systemDomain.marathon-connect"
  val marathonTimeoutTag = s"$systemDomain.marathon-connect-timeout"
  val zkSessionTimeoutTag = s"$zookeeperDomain.session-timeout"

  val jdbcTimeoutTag = s"$jdbcDomain.timeout"
  val tgClientRetryPeriodTag = s"$systemDomain.transaction-generator-client-retry-period"
  val tgServerRetryPeriodTag = s"$systemDomain.transaction-generator-server-retry-period"
  val tgRetryCountTag = s"$systemDomain.transaction-generator-retry-count"
  val kafkaSubscriberTimeoutTag = s"$systemDomain.subscriber-timeout"
  val geoIpAsNum = s"$systemDomain.geo-ip-as-num"
  val geoIpAsNumv6 = s"$systemDomain.geo-ip-as-num-v6"

  val frameworkTag = s"$systemDomain.current-framework"
  val frameworkPrincipalTag = s"$systemDomain.framework-principal"
  val frameworkSecretTag = s"$systemDomain.framework-secret"
  val frameworkBackoffSeconds = s"$systemDomain.framework-backoff-seconds"
  val frameworkBackoffFactor = s"$systemDomain.framework-backoff-factor"
  val frameworkMaxLaunchDelaySeconds = s"$systemDomain.framework-max-launch-delay-seconds"
}
