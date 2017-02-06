package com.bwsw.sj.common.utils

import java.net.{Inet4Address, Inet6Address, InetAddress}

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.maxmind.geoip.LookupService
import org.slf4j.LoggerFactory

object GeoIp {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val fileStorage = ConnectionRepository.getFileStorage
  private lazy val ipv4AsNumLookup = getAsLookupServiceIpv4
  private lazy val ipv6AsNumLookup = getAsLookupServiceIpv6

  def resolveAs(ip: String): Int = {
    try {
      InetAddress.getByName(ip) match {
        case _: Inet4Address =>
          if (ipv4AsNumLookup.getID(ip) != 0) ipv4AsNumLookup.getOrg(ip).split(" ")(0).substring(2).toInt else 0
        case _: Inet6Address =>
          if (ipv6AsNumLookup.getID(ip) != 0) ipv6AsNumLookup.getOrgV6(ip).split(" ")(0).substring(2).toInt else 0
      }
    } catch {
      case _: java.net.UnknownHostException =>
        throw new Exception(s"""resolveAs error: "$ip" isn't correct ip address""")
      case _: com.maxmind.geoip.InvalidDatabaseException =>
        logger.error(s"""resolveAs error: "$ip" com.maxmind.geoip.InvalidDatabaseException.""")
        0
    }
  }

  private def getAsLookupServiceIpv4 = {
    logger.debug("Create a geo ip lookup service of ipv4")
    val geoIpFileName = ConfigurationSettingsUtils.getGeoIpAsNumFileName()

    createLookupService(geoIpFileName)
  }

  private def getAsLookupServiceIpv6 = {
    logger.debug("Create a geo ip lookup service of ipv6")
    val geoIpFileName = ConfigurationSettingsUtils.getGeoIpAsNumv6FileName()

    createLookupService(geoIpFileName)
  }

  private def createLookupService(filename: String) = {
    val databaseFile = fileStorage.get(filename, filename)

    new LookupService(databaseFile)
  }
}