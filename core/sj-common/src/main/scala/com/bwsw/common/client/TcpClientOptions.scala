package com.bwsw.common.client

import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

/**
  * Options for TcpClient
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */

class TcpClientOptions() {
  var zkServers = Array(s"127.0.0.1:2181")
  var prefix = ""
  private val configService = ConnectionRepository.getConfigService
  var retryPeriod = configService.get(ConfigConstants.tgClientRetryPeriodTag).value.toLong
  var retryCount = configService.get(ConfigConstants.tgRetryCountTag).value.toInt

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