package com.bwsw.sj.common.utils

import java.net.{Inet4Address, Inet6Address, InetAddress}

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.maxmind.geoip.LookupService
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Wrapper for geoip repository that are contained in files with .dat extension (e.g. GeoIPASNum.dat, "GeoIPASNumv6.dat)
  * This files should be uploaded in MongoDB (with any file name)
  * but you should also create config settings to indicate what file associated with ipv4AsNumLookupService and ipv6AsNumLookupService
  *
  * {{{
  *     val configService: GenericMongoRepository[ConfigurationSettingDomain] = ConnectionRepository.getConfigRepository
  *     ConnectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "<file_name>")
  *     ConnectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")
  *
  *     configService.save(ConfigurationSettingDomain(ConfigLiterals.geoIpAsNum, "<file_name>", ConfigLiterals.systemDomain))
  *     configService.save(ConfigurationSettingDomain(ConfigLiterals.geoIpAsNumv6, "GeoIPASNumv6.dat", ConfigLiterals.systemDomain))
  * }}}
  */
object GeoIp {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val fileStorage = ConnectionRepository.getFileStorage
  private lazy val ipv4AsNumLookup = getAsLookupServiceIpv4
  private lazy val ipv6AsNumLookup = getAsLookupServiceIpv6

  def resolveAs(ip: String): Int = Try {
    InetAddress.getByName(ip) match {
      case _: Inet4Address =>
        if (ipv4AsNumLookup.getID(ip) != 0) ipv4AsNumLookup.getOrg(ip).split(" ")(0).substring(2).toInt else 0
      case _: Inet6Address =>
        if (ipv6AsNumLookup.getID(ip) != 0) ipv6AsNumLookup.getOrgV6(ip).split(" ")(0).substring(2).toInt else 0
    }
  } match {
    case Success(as) => as
    case Failure(_: java.net.UnknownHostException) =>
      throw new Exception(s"""resolveAs error: "$ip" isn't correct ip address""")
    case Failure(_: com.maxmind.geoip.InvalidDatabaseException) =>
      logger.error(s"""resolveAs error: "$ip" com.maxmind.geoip.InvalidDatabaseException.""")
      0
    case Failure(e) => throw e
  }


  private def getAsLookupServiceIpv4: LookupService = {
    logger.debug("Create a geo ip lookup service of ipv4")
    val geoIpFileName = ConfigurationSettingsUtils.getGeoIpAsNumFileName()

    createLookupService(geoIpFileName)
  }

  private def getAsLookupServiceIpv6: LookupService = {
    logger.debug("Create a geo ip lookup service of ipv6")
    val geoIpFileName = ConfigurationSettingsUtils.getGeoIpAsNumv6FileName()

    createLookupService(geoIpFileName)
  }

  private def createLookupService(filename: String): LookupService = {
    val databaseFile = fileStorage.get(filename, filename)

    new LookupService(databaseFile)
  }
}