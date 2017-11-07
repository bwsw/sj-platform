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

import com.bwsw.sj.common.config.ConfigLiterals._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.utils.{EngineLiterals, FrameworkLiterals}
import scaldi.Injectable.inject
import scaldi.Injector

class SettingsUtils(implicit val injector: Injector) {
  private val configService = inject[ConnectionRepository].getConfigRepository

  def getKafkaSubscriberTimeout(): Int = {
    getIntConfigSetting(kafkaSubscriberTimeoutTag)
  }

  def getZkSessionTimeout(): Int = {
    getIntConfigSetting(zkSessionTimeoutTag, Some(ConfigLiterals.zkSessionTimeoutDefault))
  }

  def getFrameworkJarName(): String = {
    val currentFrameworkName = getStringConfigSetting(frameworkTag)
    val currentFramework = ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, currentFrameworkName)

    getStringConfigSetting(currentFramework)
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

  def getLowWatermark(): Int = {
    getIntConfigSetting(lowWatermark)
  }

  def getJdbcDriverFilename(driverName: String): String = {
    val driverFilename = ConfigLiterals.getDriverFilename(driverName)

    getStringConfigSetting(driverFilename)
  }

  def getJdbcDriverClass(driverName: String): String = {
    val driverClass = ConfigLiterals.getDriverClass(driverName)

    getStringConfigSetting(driverClass)
  }

  def getJdbcDriverPrefix(driverName: String): String = {
    val driverPrefix = ConfigLiterals.getDriverPrefix(driverName)

    getStringConfigSetting(driverPrefix)
  }

  def getBackoffSettings(): (Int, Double, Int) = {
    (getFrameworkBackoffSeconds(), getFrameworkBackoffFactor(), getFrameworkMaxLaunchDelaySeconds())
  }

  def getOutputProcessorParallelism(): Int = {
    getIntConfigSetting(outputProcessorParallelism, Some(EngineLiterals.outputProcessorParallelism))
  }

  def getFrameworkPrincipal(): Option[String] = {
    getConfigSettings(frameworkPrincipalTag)
  }

  def getFrameworkSecret(): Option[String] = {
    getConfigSettings(frameworkSecretTag)
  }

  private def getFrameworkBackoffSeconds(): Int = {
    getIntConfigSetting(frameworkBackoffSeconds, Some(FrameworkLiterals.defaultBackoffSeconds))
  }

  private def getFrameworkBackoffFactor(): Double = {
    getDoubleConfigSetting(frameworkBackoffFactor, Some(FrameworkLiterals.defaultBackoffFactor))
  }

  private def getFrameworkMaxLaunchDelaySeconds(): Int = {
    getIntConfigSetting(frameworkMaxLaunchDelaySeconds, Some(FrameworkLiterals.defaultMaxLaunchDelaySeconds))
  }

  private def getIntConfigSetting(name: String, defaultValue: Option[Int] = None): Int = {
    getConfigSettings(name).map(_.toInt).getOrElse(defaultValue.getOrElse(throw NoSuchConfigException(name)))
  }

  private def getStringConfigSetting(name: String, defaultValue: Option[String] = None): String = {
    getConfigSettings(name).getOrElse(defaultValue.getOrElse(throw NoSuchConfigException(name)))
  }

  private def getDoubleConfigSetting(name: String, defaultValue: Option[Double] = None): Double = {
    getConfigSettings(name).map(_.toDouble).getOrElse(defaultValue.getOrElse(throw NoSuchConfigException(name)))
  }

  private def getConfigSettings(name: String): Option[String] = {
    configService.get(name).map(_.value)
  }
}
