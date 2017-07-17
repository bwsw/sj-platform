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

object TempHelperForConfigSetup {

  import SjModule._
  import TempHelperForConfigConstants._

  val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  val configService: GenericMongoRepository[ConfigurationSettingDomain] = connectionRepository.getConfigRepository

  def setupConfigs(): Unit = {
    configService.save(ConfigurationSettingDomain(ConfigLiterals.frameworkTag, "com.bwsw.fw-1.0", ConfigLiterals.systemDomain))

  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "regular-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.instance.validator.RegularInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "batch-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.instance.validator.BatchInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "output-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.instance.validator.OutputInstanceValidator", ConfigLiterals.systemDomain))
  configService.save(ConfigurationSettingDomain(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, "input-streaming-validator-class"),
    "com.bwsw.sj.crud.rest.instance.validator.InputInstanceValidator", ConfigLiterals.systemDomain))

    configService.save(ConfigurationSettingDomain(
      ConfigLiterals.marathonTag,
      "http://stream-juggler.z1.netpoint-dc.com:8080",
      ConfigLiterals.systemDomain))

    configService.save(ConfigurationSettingDomain(ConfigLiterals.marathonTimeoutTag, "60000", ConfigLiterals.systemDomain))

    configService.save(ConfigurationSettingDomain(ConfigLiterals.zkSessionTimeoutTag, "7000", ConfigLiterals.zookeeperDomain))

    //configService.save(new ConfigurationSetting("session.timeout.ms", "30000", ConfigConstants.kafkaDomain))

    configService.save(ConfigurationSettingDomain(ConfigLiterals.kafkaSubscriberTimeoutTag, "100", ConfigLiterals.systemDomain))
    configService.save(ConfigurationSettingDomain(ConfigLiterals.lowWatermark, "100", ConfigLiterals.systemDomain))
  }

  def loadJdbcDriver(): Unit = {
    val driver: File = new File(driverFileName)
    FileUtils.copyURLToFile(
      new URL("http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.41/mysql-connector-java-5.1.41.jar"),
      driver)
    connectionRepository.getFileStorage.put(
      driver,
      driverFileName,
      Map("description" -> RestLiterals.defaultDescription),
      FileMetadataLiterals.customFileType)
    configService.save(ConfigurationSettingDomain(driverNameConfig, driverFileName, ConfigLiterals.jdbcDomain))
    configService.save(ConfigurationSettingDomain(driverClassConfig, "com.mysql.jdbc.Driver", ConfigLiterals.jdbcDomain))
    configService.save(ConfigurationSettingDomain(driverPrefixConfig, "jdbc:mysql", ConfigLiterals.jdbcDomain))

    driver.delete()
  }
}

object TempHelperForConfigDestroy {

  import SjModule._
  import TempHelperForConfigConstants._

  val connectionRepository: ConnectionRepository = inject[ConnectionRepository]

  def deleteConfigs(): Unit = {
    connectionRepository.getConfigRepository.delete(ConfigLiterals.frameworkTag)
    connectionRepository.getConfigRepository.delete(
      ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, regularStreamingValidatorClass))
    connectionRepository.getConfigRepository.delete(
      ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, batchStreamingValidatorClass))
    connectionRepository.getConfigRepository.delete(
      ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, outputStreamingValidatorClass))
    connectionRepository.getConfigRepository.delete(
      ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, inputStreamingValidatorClass))
    connectionRepository.getConfigRepository.delete(ConfigLiterals.marathonTag)
    connectionRepository.getConfigRepository.delete(ConfigLiterals.marathonTimeoutTag)
    connectionRepository.getConfigRepository.delete(ConfigLiterals.zkSessionTimeoutTag)
    connectionRepository.getConfigRepository.delete(ConfigLiterals.kafkaSubscriberTimeoutTag)
    connectionRepository.getConfigRepository.delete(ConfigLiterals.lowWatermark)
  }

  def deleteJdbcDriver(): Boolean = {
    connectionRepository.getConfigRepository.delete(
      ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.jdbcDomain, driverNameConfig))
    connectionRepository.getConfigRepository.delete(
      ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.jdbcDomain, driverClassConfig))
    connectionRepository.getConfigRepository.delete(
      ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.jdbcDomain, driverPrefixConfig))

    connectionRepository.getFileStorage.delete(driverFileName)
  }
}

object TempHelperForConfigConstants {
  val regularStreamingValidatorClass: String = "regular-streaming-validator-class"
  val batchStreamingValidatorClass: String = "batch-streaming-validator-class"
  val inputStreamingValidatorClass: String = "input-streaming-validator-class"
  val outputStreamingValidatorClass: String = "output-streaming-validator-class"

  val driverName: String = "mysql"
  val driverFileName: String = s"$driverName.jar"
  val driverNameConfig: String = s"${ConfigLiterals.jdbcDriver}.$driverName"
  val driverClassConfig: String = s"${ConfigLiterals.jdbcDriver}.$driverName.class"
  val driverPrefixConfig: String = s"${ConfigLiterals.jdbcDriver}.$driverName.prefix"
}