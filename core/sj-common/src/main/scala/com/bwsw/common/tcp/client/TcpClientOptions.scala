package com.bwsw.common.tcp.client

import com.bwsw.sj.common.config.ConfigurationSettingsUtils

class TcpClientOptions() {
  var zkServers = "127.0.0.1:2181"
  var prefix = ""

  var retryPeriod = ConfigurationSettingsUtils.getClientRetryPeriod()
  var retryCount = ConfigurationSettingsUtils.getRetryCount()

  def setZkServers(hosts: Array[String]): TcpClientOptions = {
    zkServers = hosts.mkString(",")
    this
  }

  def setPrefix(pref: String): TcpClientOptions = {
    prefix = pref
    this
  }

  def setRetryPeriod(period: Int): TcpClientOptions = {
    retryPeriod = period
    this
  }

  def setRetryCount(count: Int): TcpClientOptions = {
    retryCount = count
    this
  }
}