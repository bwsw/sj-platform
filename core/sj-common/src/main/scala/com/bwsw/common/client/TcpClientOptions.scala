package com.bwsw.common.client

import com.bwsw.sj.common.utils.ConfigSettingsUtils

class TcpClientOptions() {
  var zkServers = "127.0.0.1:2181"
  var prefix = ""

  var retryPeriod = ConfigSettingsUtils.getClientRetryPeriod()
  var retryCount = ConfigSettingsUtils.getRetryCount()

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