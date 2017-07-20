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
package com.bwsw.sj.crud.rest.controller

import java.util.Date

import com.bwsw.common.JsonSerializer
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals._
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ConfigSettingsSI
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.model.config.{ConfigurationSettingApi, ConfigurationSettingApiCreator}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.{ConfigSettingResponseEntity, ConfigSettingsResponseEntity}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.Module

import scala.collection.mutable.ArrayBuffer

class ConfigSettingsControllerTests extends FlatSpec with Matchers with MockitoSugar {
  val entityDeletedMessageName = "rest.config.setting.deleted"
  val entityNotFoundMessageName = "rest.config.setting.notfound"
  val createdMessageName = "rest.config.setting.created"
  val cannotCreateMessageName = "rest.config.setting.cannot.create"
  val unknownDomainMessageName = "rest.config.setting.domain.unknown"

  val creationError = ArrayBuffer("not created")

  val serializer = mock[JsonSerializer]

  val messageResourceUtils = mock[MessageResourceUtils]
  val createConfigurationSettingApi = mock[ConfigurationSettingApiCreator]

  val serviceInterface = mock[ConfigSettingsSI]
  when(serviceInterface.get(anyString())).thenReturn(None)
  when(serviceInterface.delete(anyString())).thenReturn(EntityNotFound)
  when(serviceInterface.create(any[ConfigurationSetting]())).thenReturn(NotCreated(creationError))

  val jsonDeserializationErrorMessageCreator = mock[JsonDeserializationErrorMessageCreator]

  val systemConfigsCount = 3
  val systemConfigs = Range(0, systemConfigsCount).map(i => createConfigSetting(s"name$i", s"value$i", systemDomain))
  val tStreamConfigsCount = 4
  val tStreamConfigs = Range(0, tStreamConfigsCount).map(i => createConfigSetting(s"name$i", s"value$i", tstreamsDomain))

  val configsByDomain = Map(
    systemDomain -> systemConfigs,
    tstreamsDomain -> tStreamConfigs)
  configsByDomain.foreach {
    case (domain, configs) =>
      when(serviceInterface.getBy(domain)).thenReturn(configs.map(_.config).toBuffer)
  }

  val allConfigs = configsByDomain.values.flatten
  allConfigs.foreach {
    case ConfigSettingInfo(_, _, config, _, _, domainName) =>
      when(serviceInterface.get(domainName)).thenReturn(Some(config))
      when(serviceInterface.delete(domainName)).thenReturn(Deleted)
  }
  when(serviceInterface.getAll()).thenReturn(allConfigs.map(_.config).toBuffer)

  val entityDeletedMessages = allConfigs.map {
    case ConfigSettingInfo(_, _, _, name, domain, domainName) =>
      val message = entityDeletedMessageName + "," + domainName
      when(messageResourceUtils.createMessage(entityDeletedMessageName, domainName)).thenReturn(message)

      (name, domain, domainName, message)
  }

  val newConfigName = "new-config-name"
  val newConfigValue = "new-config-value"
  val newConfigDomain = systemDomain
  val newConfig = createConfigSetting(newConfigName, newConfigValue, newConfigDomain)
  when(serviceInterface.create(newConfig.config)).thenReturn(Created)

  val createdMessage = createdMessageName + "," + newConfigName
  when(messageResourceUtils.createMessage(createdMessageName, newConfigName)).thenReturn(createdMessage)

  val entityNotFoundMessage = entityNotFoundMessageName + "," + newConfig.domainName
  when(messageResourceUtils.createMessage(entityNotFoundMessageName, newConfig.domainName))
    .thenReturn(entityNotFoundMessage)

  val notValidConfigName = "not-valid-config-name"
  val notValidConfigValue = "not-valid-config-value"
  val notValidConfigDomain = "not-valid-domain"
  val notValidConfig = createConfigSetting(notValidConfigName, notValidConfigValue, notValidConfigDomain)

  val notValidMessage = cannotCreateMessageName + "," + notValidConfigName
  when(messageResourceUtils.createMessageWithErrors(cannotCreateMessageName, creationError)).thenReturn(notValidMessage)

  val notDeletedConfigName = "not-deleted-config-name"
  val notDeletedConfigValue = "not-deleted-config-value"
  val notDeletedConfigDomain = systemDomain
  val notDeletedConfig = createConfigSetting(notDeletedConfigName, notDeletedConfigValue, notDeletedConfigDomain)
  val deletionError = "config-setting-not-deleted"
  when(serviceInterface.delete(notDeletedConfig.domainName)).thenReturn(DeletionError(deletionError))

  val incorrectJson = "{not a json}"
  val incorrectJsonException = new JsonDeserializationException("json is incorrect")
  val incorrectJsonError = "error: json is incorrect"
  when(serializer.deserialize[ConfigurationSettingApi](incorrectJson)).thenAnswer(_ => throw incorrectJsonException)
  when(jsonDeserializationErrorMessageCreator.apply(incorrectJsonException)).thenReturn(incorrectJsonError)

  val incorrectJsonMessage = cannotCreateMessageName + "," + incorrectJson
  when(messageResourceUtils.createMessage(cannotCreateMessageName, incorrectJsonError)).thenReturn(incorrectJsonMessage)

  val unknownDomain = "unknown-domain"
  val unknownDomainMessage = unknownDomainMessageName + "," + unknownDomain
  val s = domains.mkString(", ")
  when(messageResourceUtils.createMessage(unknownDomainMessageName, unknownDomain, s))
    .thenReturn(unknownDomainMessage)

  val injector = new Module {
    bind[JsonSerializer] to serializer
    bind[MessageResourceUtils] to messageResourceUtils
    bind[JsonDeserializationErrorMessageCreator] to jsonDeserializationErrorMessageCreator
    bind[ConfigSettingsSI] to serviceInterface
    bind[ConfigurationSettingApiCreator] to createConfigurationSettingApi
  }.injector

  val controller = new ConfigSettingsController()(injector)


  // create
  "ConfigSettingsController" should "tell that valid configuration created" in {
    val expected = CreatedRestResponse(MessageResponseEntity(createdMessage))
    controller.create(newConfig.serialized) shouldBe expected
  }

  it should "tell that configuration setting is not a valid" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(notValidMessage))
    controller.create(notValidConfig.serialized) shouldBe expected
  }

  it should "tell that serialized to json configuration setting is not a correct" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(incorrectJsonMessage))
    controller.create(incorrectJson) shouldBe expected
  }

  // get(name: String)
  it should "give configuration setting if it is exists" in {
    allConfigs.foreach {
      case ConfigSettingInfo(_, api, _, _, _, domainName) =>
        controller.get(domainName) shouldBe OkRestResponse(ConfigSettingResponseEntity(api))
    }
  }

  it should "not give configuration setting if it does not exists" in {
    controller.get(newConfig.domainName) shouldBe NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
  }

  // get(domain: String, name: String)
  it should "give configuration setting by domain and name if it is exists" in {
    allConfigs.foreach {
      case ConfigSettingInfo(_, api, _, name, domain, _) =>
        controller.get(domain, name) shouldBe OkRestResponse(ConfigSettingResponseEntity(api))
    }
  }

  it should "not give configuration setting if it does not exists and domain is correct" in {
    val expected = NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
    controller.get(newConfigDomain, newConfigName) shouldBe expected
  }

  it should "not give configuration setting by domain and name if domain is incorrect" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(unknownDomainMessage))
    controller.get(unknownDomain, newConfigName) shouldBe expected
  }

  // getAll
  it should "give all configuration settings" in {
    val expected = OkRestResponse(ConfigSettingsResponseEntity(allConfigs.map(_.api).toBuffer))
    controller.getAll() shouldBe expected
  }

  // getByDomain
  it should "give all configuration settings with specific domain" in {
    configsByDomain.foreach {
      case (domain, configs) =>
        val expected = OkRestResponse(ConfigSettingsResponseEntity(configs.map(_.api).toBuffer))
        controller.getByDomain(domain) shouldBe expected
    }
  }

  it should "not give configuration settings for incorrect domain" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(unknownDomainMessage))
    controller.getByDomain(unknownDomain) shouldBe expected
  }

  // delete(name: String)
  it should "delete configuration setting if it exists" in {
    entityDeletedMessages.foreach {
      case (_, _, domainName, message) =>
        controller.delete(domainName) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete configuration setting if it does not exists" in {
    controller.delete(newConfig.domainName) shouldBe NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
  }

  it should "report in a case some deletion error (delete(name))" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.delete(notDeletedConfig.domainName) shouldBe expected
  }

  // delete(domain: String, name: String)
  it should "delete configuration setting by domain and name if it exists" in {
    entityDeletedMessages.foreach {
      case (name, domain, _, message) =>
        controller.delete(domain, name) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete configuration setting by domain and name if it does not exists" in {
    val expected = NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
    controller.delete(newConfigDomain, newConfigName) shouldBe expected
  }

  it should "report in a case some deletion error (delete(domain, name))" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.delete(notDeletedConfigDomain, notDeletedConfigName) shouldBe expected
  }

  it should "not delete configuration setting by domain and name if domain is incorrect" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(unknownDomainMessage))
    controller.delete(unknownDomain, newConfigName) shouldBe expected
  }


  def createConfigSetting(name: String, value: String, domain: String) = {
    val serialized = s"""{"domain":"$domain","name":"$name","value":"$value"}"""
    val domainName = domain + "." + name
    val config = new ConfigurationSetting(name, value, domain, new Date().toString)
    val api = spy(new ConfigurationSettingApi(name, value, domain, new Date().toString))
    when(api.to()).thenReturn(config)
    when(serializer.deserialize[ConfigurationSettingApi](serialized)).thenReturn(api)
    when(createConfigurationSettingApi.from(config)).thenReturn(api)

    ConfigSettingInfo(serialized, api, config, name, domain, domainName)
  }

  case class ConfigSettingInfo(serialized: String,
                               api: ConfigurationSettingApi,
                               config: ConfigurationSetting,
                               name: String,
                               domain: String,
                               domainName: String)

}
