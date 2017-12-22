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
package com.bwsw.sj.common.si

import java.util.Date

import com.bwsw.sj.common.config.ConfigLiterals.{domains, systemDomain}
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.config.{ConfigurationSetting, ConfigurationSettingCreator}
import com.bwsw.sj.common.si.result._
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable.ArrayBuffer

class ConfigSettingsSiTests extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  val createConfigurationSetting = mock[ConfigurationSettingCreator]

  val configPerDomainCount = 3

  val configsInStorageByDomain = domains.map { domain =>
    val configs: Seq[(ConfigurationSetting, ConfigurationSettingDomain)] = Range(0, configPerDomainCount).map { index =>
      val configName = "name-" + index
      val configValue = "value-" + index
      val configNameWithDomain = domain + "." + configName
      val configurationSettingDomain = ConfigurationSettingDomain(configNameWithDomain, configValue, domain, new Date())
      val configurationSetting = mock[ConfigurationSetting]
      when(configurationSetting.domain).thenReturn(domain)
      when(configurationSetting.name).thenReturn(configName)
      when(configurationSetting.value).thenReturn(configValue)
      when(configurationSetting.to()).thenReturn(configurationSettingDomain)

      when(createConfigurationSetting.from(configurationSettingDomain)).thenReturn(configurationSetting)

      (configurationSetting, configurationSettingDomain)
    }

    (domain, configs)
  }

  val configsInStorage = configsInStorageByDomain.flatMap(_._2)


  val configNotInStorageName = "not-in-storage-name"
  val configNotInStorageValue = "not-in-storage-value"
  val configNotInStorageConfigDomain = systemDomain
  val configNotInStorageNameWithDomain = configNotInStorageConfigDomain + "." + configNotInStorageName
  val configNotInStorageDomain = ConfigurationSettingDomain(
    configNotInStorageNameWithDomain,
    configNotInStorageValue,
    configNotInStorageConfigDomain,
    new Date())
  val configNotInStorage = mock[ConfigurationSetting]
  when(configNotInStorage.domain).thenReturn(configNotInStorageConfigDomain)
  when(configNotInStorage.name).thenReturn(configNotInStorageName)
  when(configNotInStorage.value).thenReturn(configNotInStorageValue)
  when(configNotInStorage.to()).thenReturn(configNotInStorageDomain)

  val configRepository = mock[GenericMongoRepository[ConfigurationSettingDomain]]
  val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getConfigRepository).thenReturn(configRepository)

  val module = new Module {
    bind[ConnectionRepository] to connectionRepository
    bind[ConfigurationSettingCreator] to createConfigurationSetting
  }
  implicit val injector = module.injector

  val configSettingsSI = new ConfigSettingsSI()(injector)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(configRepository)
    when(configRepository.get(anyString())).thenReturn(None)

    configsInStorageByDomain.foreach {
      case (domain, configs) =>
        val configDomains = configs.unzip._2
        when(configRepository.getByParameters(Map("domain" -> domain))).thenReturn(configDomains)
        configDomains.foreach { configurationSettingDomain =>
          when(configRepository.get(configurationSettingDomain.name)).thenReturn(Some(configurationSettingDomain))
        }
    }
    when(configRepository.getAll).thenReturn(configsInStorage.map(_._2).toBuffer)
  }

  "ConfigSettingsSI" should "create correct configuration setting" in {
    when(configNotInStorage.validate()(any[Injector])).thenReturn(ArrayBuffer[String]())

    configSettingsSI.create(configNotInStorage) shouldBe Created
    verify(configRepository).save(configNotInStorageDomain)
  }

  it should "not create incorrect configuration setting" in {
    val errors = ArrayBuffer("Not valid")
    when(configNotInStorage.validate()(any[Injector])).thenReturn(errors)

    configSettingsSI.create(configNotInStorage) shouldBe NotCreated(errors)
    verify(configRepository, never()).save(any[ConfigurationSettingDomain]())
  }

  it should "give all configuration settings" in {
    configSettingsSI.getAll().toSet shouldBe configsInStorage.map(_._1).toSet
  }

  it should "give configuration setting if it exists in storage" in {
    configsInStorage.foreach {
      case (configurationSetting, configurationSettingDomain) =>
        configSettingsSI.get(configurationSettingDomain.name) shouldBe Some(configurationSetting)
    }
  }

  it should "not give configuration setting if does not exists in storage" in {
    configSettingsSI.get(configNotInStorageNameWithDomain) shouldBe empty
  }

  it should "delete configuration setting if it exists in storage" in {
    configsInStorage.foreach {
      case (_, configurationSettingDomain) =>
        val name = configurationSettingDomain.name
        configSettingsSI.delete(name) shouldBe Deleted
        verify(configRepository).delete(name)
    }
  }

  it should "not delete configuration setting if does not exists in storage" in {
    configSettingsSI.delete(configNotInStorageNameWithDomain) shouldBe EntityNotFound
    verify(configRepository, never()).delete(configNotInStorageNameWithDomain)
  }

  it should "give all configuration settings with specific config domain" in {
    configsInStorageByDomain.foreach {
      case (domain, configs) =>
        val configurationSettings = configs.unzip._1
        configSettingsSI.getBy(domain).toSet shouldBe configurationSettings.toSet
    }
  }
}
