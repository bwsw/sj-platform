package com.bwsw.sj.common.utils

private[common] object SjStreamUtilsForCreation {

  private def splitHosts(hosts: Array[String]) = {
    hosts.map(s => {
      val address = s.split(":")
      (address(0), address(1).toInt)
    }).toSet
  }
}
