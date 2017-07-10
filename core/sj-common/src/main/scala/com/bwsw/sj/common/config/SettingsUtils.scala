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

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals._
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.utils.FrameworkLiterals
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class SettingsUtils(implicit val injector: Injector) {
  private val configService = inject[ConnectionRepository].getConfigRepository

  def getKafkaSubscriberTimeout(): Int = {
    getIntConfigSetting(kafkaSubscriberTimeoutTag)
  }

  def getZkSessionTimeout(): Int = {
    getIntConfigSetting(zkSessionTimeoutTag)
  }

  def getFrameworkJarName(): String = {
    getStringConfigSetting(
      ConfigurationSetting.createConfigurationSettingName(
        ConfigLiterals.systemDomain,
        getStringConfigSetting(frameworkTag)
      )
    )
  }

  def getCrudRestHost(): String = {
    getStringConfigSetting(hostOfCrudRestTag)
  }

  def getCrudRestPort(): Int = {
    getIntConfigSetting(portOfCrudRestTag)
  }

  def getMarathonConnect(): String = {
    getStringConfigSetting(marathonTag)
  }

  def getMarathonTimeout(): Int = {
    getIntConfigSetting(marathonTimeoutTag)
  }

  def getFrameworkBackoffSeconds(): Int = {
    getIntConfigSetting(frameworkBackoffSeconds)
  }

  def getFrameworkBackoffFactor(): Double = {
    getDoubleConfigSetting(frameworkBackoffFactor)
  }

  def getFrameworkMaxLaunchDelaySeconds(): Int = {
    getIntConfigSetting(frameworkMaxLaunchDelaySeconds)
  }

  def getLowWatermark(): Int = {
    getIntConfigSetting(lowWatermark)
  }

  def getJdbcDriverFileName(driverName: String): String = getStringConfigSetting(s"$jdbcDriver.$driverName")

  def getJdbcDriverClass(driverName: String): String = getStringConfigSetting(s"$jdbcDriver.$driverName.class")

  def getJdbcDriverPrefix(driverName: String): String = getStringConfigSetting(s"$jdbcDriver.$driverName.prefix")

  def getBackoffSettings(): (Int, Double, Int) = {
    val backoffSeconds = Try(getFrameworkBackoffSeconds()) match {
      case Success(x) => x
      case Failure(_: NoSuchFieldException) => FrameworkLiterals.defaultBackoffSeconds
      case Failure(e) => throw e
    }
    val backoffFactor = Try(getFrameworkBackoffFactor()) match {
      case Success(x) => x
      case Failure(_: NoSuchFieldException) => FrameworkLiterals.defaultBackoffFactor
      case Failure(e) => throw e
    }
    val maxLaunchDelaySeconds = Try(getFrameworkMaxLaunchDelaySeconds()) match {
      case Success(x) => x
      case Failure(_: NoSuchFieldException) => FrameworkLiterals.defaultMaxLaunchDelaySeconds
      case Failure(e) => throw e
    }
    (backoffSeconds, backoffFactor, maxLaunchDelaySeconds)
  }

  private def getIntConfigSetting(name: String): Int = {
    getConfigSettings(name).toInt
  }

  private def getStringConfigSetting(name: String): String = {
    getConfigSettings(name)
  }

  private def getDoubleConfigSetting(name: String): Double = {
    getConfigSettings(name).toDouble
  }

  private def getConfigSettings(name: String): String = {
    val maybeSetting = configService.get(name)
    if (maybeSetting.isEmpty)
      throw new NoSuchFieldException(s"Config setting is named '$name' has not found")
    else
      maybeSetting.get.value
  }
}
