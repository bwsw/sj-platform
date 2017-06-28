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
package com.bwsw.sj.common.config

import java.io.File
import java.net.URL

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadataLiterals
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.utils.RestLiterals
import org.apache.commons.io.FileUtils
import scaldi.Injectable.inject

object TempHelperForConfigSetup extends App {

  import SjModule._

  val connectionRepository: ConnectionRepository = inject[ConnectionRepository]

  //connectionRepository.getFileStorage.put(new File("GeoIPASNum.dat"), "GeoIPASNum.dat")
  //connectionRepository.getFileStorage.put(new File("GeoIPASNumv6.dat"), "GeoIPASNumv6.dat")

  val configService: GenericMongoRepository[ConfigurationSettingDomain] = connectionRepository.getConfigRepository

  configService.save(ConfigurationSettingDomain(ConfigLiterals.frameworkTag, "com.bwsw.fw-1.0", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.RegularInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.BatchInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.OutputInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.validator.instance.InputInstanceValidator", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigLiterals.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.marathonTimeoutTag, "60000", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigLiterals.zkSessionTimeoutTag, "7000", ConfigLiterals.zookeeperDomain))

  //configService.save(new ConfigurationSetting("session.timeout.ms", "30000", ConfigConstants.kafkaDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.jdbcTimeoutTag, "6000", ConfigLiterals.jdbcDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.restTimeoutTag, "5000", ConfigLiterals.restDomain))

  configService.save(ConfigurationSettingDomain(ConfigLiterals.kafkaSubscriberTimeoutTag, "100", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigLiterals.lowWatermark, "100", ConfigLiterals.systemDomain))

  //  configService.save(ConfigurationSettingDomain(ConfigLiterals.geoIpAsNum, "GeoIPASNum.dat", ConfigLiterals.systemDomain))
  //  configService.save(ConfigurationSettingDomain(ConfigLiterals.geoIpAsNumv6, "GeoIPASNumv6.dat", ConfigLiterals.systemDomain))

  val driverName: String = "mysql"
  val driverFileName: String = s"$driverName.jar"
  private val driver: File = new File(driverFileName)
  FileUtils.copyURLToFile(
    new URL("http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.41/mysql-connector-java-5.1.41.jar"),
    driver)
  connectionRepository.getFileStorage.put(driver, driverFileName, Map("description" -> RestLiterals.defaultDescription), FileMetadataLiterals.customFileType)
  configService.save(ConfigurationSettingDomain(s"${ConfigLiterals.jdbcDriver}.$driverName", driverFileName, ConfigLiterals.jdbcDomain))
  configService.save(ConfigurationSettingDomain(s"${ConfigLiterals.jdbcDriver}.$driverName.class", "com.mysql.jdbc.Driver", ConfigLiterals.jdbcDomain))
  configService.save(ConfigurationSettingDomain(s"${ConfigLiterals.jdbcDriver}.$driverName.prefix", "jdbc:mysql", ConfigLiterals.jdbcDomain))
}

object TempHelperForConfigDestroy extends App {

  import SjModule._

  val connectionRepository: ConnectionRepository = inject[ConnectionRepository]

  connectionRepository.getConfigRepository.delete(ConfigLiterals.frameworkTag)
  connectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"))
  connectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"))
  connectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"))
  connectionRepository.getConfigRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"))
  connectionRepository.getConfigRepository.delete(ConfigLiterals.marathonTag)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.marathonTimeoutTag)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.zkSessionTimeoutTag)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.kafkaSubscriberTimeoutTag)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.lowWatermark)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.geoIpAsNum)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.geoIpAsNumv6)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.jdbcTimeoutTag)
  connectionRepository.getConfigRepository.delete(ConfigLiterals.restTimeoutTag)

  val driverName: String = "mysql"
  connectionRepository.getConfigRepository.delete(s"${ConfigLiterals.jdbcDriver}.$driverName")
  connectionRepository.getConfigRepository.delete(s"${ConfigLiterals.jdbcDriver}.$driverName.class")
  connectionRepository.getConfigRepository.delete(s"${ConfigLiterals.jdbcDriver}.$driverName.prefix")

  connectionRepository.getFileStorage.delete(s"$driverName.jar")
}