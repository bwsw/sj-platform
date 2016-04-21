package com.bwsw.sj.transaction.generator.client

/**
  * Options for TcpClient
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */

class TcpClientOptions() {
  var zkServers = Array(s"zk127.0.0.1:2181")
  var prefix = ""
  var retryPeriod: Long = 500
  var retryCount: Int = 3

  def setZkServers(hosts: Array[String]): TcpClientOptions = {
    zkServers = hosts
    this
  }

  def setPrefix(pref: String): TcpClientOptions = {
    prefix = pref
    this
  }

  def setRetryPeriod(period: Long): TcpClientOptions = {
    retryPeriod = period
    this
  }

  def setRetryCount(count: Int): TcpClientOptions = {
    retryCount = count
    this
  }

}
