package com.bwsw.common.client

import com.bwsw.sj.common.utils.ConfigUtils

/**
  * Options for TcpClient
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */

class TcpClientOptions() {
  var zkServers = Array(s"127.0.0.1:2181")
  var prefix = ""

  var retryPeriod = ConfigUtils.getClientRetryPeriod()
  var retryCount = ConfigUtils.getRetryCount()

  def setZkServers(hosts: Array[String]): TcpClientOptions = {
    zkServers = hosts
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