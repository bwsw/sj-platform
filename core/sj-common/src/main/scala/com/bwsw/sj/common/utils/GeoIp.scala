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
package com.bwsw.sj.common.utils

import java.io.File
import java.net.{Inet4Address, Inet6Address, InetAddress}

import com.maxmind.geoip.LookupService
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Wrapper for geoip repository that are contained in files with .dat extension (e.g. GeoIPASNum.dat, GeoIPASNumv6.dat)
  *
  * @param ipv4DatabaseFile geoip database file for IPv4
  * @param ipv6DatabaseFile geoip database file for IPv4
  */
class GeoIp(ipv4DatabaseFile: Option[File] = None, ipv6DatabaseFile: Option[File] = None) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private lazy val ipv4AsNumLookup = getAsLookupServiceIpv4
  private lazy val ipv6AsNumLookup = getAsLookupServiceIpv6

  def resolveAs(ip: String): Int = Try {
    InetAddress.getByName(ip) match {
      case address: Inet4Address =>
        if (ipv4AsNumLookup.getID(address) != 0)
          ipv4AsNumLookup.getOrg(address).split(" ")(0).substring(2).toInt
        else 0

      case address: Inet6Address =>
        if (ipv6AsNumLookup.getID(address) != 0)
          ipv6AsNumLookup.getOrgV6(address).split(" ")(0).substring(2).toInt
        else 0
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

    new LookupService(ipv4DatabaseFile.get, LookupService.GEOIP_INDEX_CACHE)
  }

  private def getAsLookupServiceIpv6: LookupService = {
    logger.debug("Create a geo ip lookup service of ipv6")

    new LookupService(ipv6DatabaseFile.get, LookupService.GEOIP_INDEX_CACHE)
  }
}