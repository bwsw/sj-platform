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

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ConfigSettingsSI
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.config.{ConfigurationSettingApi, CreateConfigurationSettingApi}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ConfigSettingsController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  val serviceInterface = inject[ConfigSettingsSI]
  private val createConfigurationSettingApi = inject[CreateConfigurationSettingApi]

  override protected val entityDeletedMessage: String = "rest.config.setting.deleted"
  override protected val entityNotFoundMessage: String = "rest.config.setting.notfound"

  def get(name: String): RestResponse = {
    serviceInterface.get(name) match {
      case Some(x) =>
        OkRestResponse(ConfigSettingResponseEntity(createConfigurationSettingApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def get(domain: String, name: String): RestResponse =
    ifDomainCorrect(domain)(get(ConfigurationSetting.createConfigurationSettingName(domain, name)))

  def getAll(): RestResponse = {
    val configElements = serviceInterface.getAll().map(createConfigurationSettingApi.from)
    OkRestResponse(ConfigSettingsResponseEntity(configElements))
  }

  def create(serializedEntity: String): RestResponse = {
    Try(serializer.deserialize[ConfigurationSettingApi](serializedEntity)) match {
      case Success(configData) =>
        val created = serviceInterface.create(configData.to())
        created match {
          case Created =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.config.setting.created", configData.name)))
          case NotCreated(errors) => BadRequestRestResponse(MessageResponseEntity(
            createMessageWithErrors("rest.config.setting.cannot.create", errors)
          ))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = jsonDeserializationErrorMessageCreator(exception)
        BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.config.setting.cannot.create", error)))

      case Failure(exception) => throw exception
    }
  }

  def getByDomain(domain: String): RestResponse = {
    ifDomainCorrect(domain) {
      val configElements = serviceInterface.getBy(domain).map(createConfigurationSettingApi.from)
      OkRestResponse(ConfigSettingsResponseEntity(configElements))
    }
  }

  def delete(domain: String, name: String): RestResponse =
    ifDomainCorrect(domain)(delete(ConfigurationSetting.createConfigurationSettingName(domain, name)))

  private def ifDomainCorrect(domain: String)(f: => RestResponse): RestResponse = {
    if (!ConfigLiterals.domains.contains(domain))
      BadRequestRestResponse(
        MessageResponseEntity(
          createMessage("rest.config.setting.domain.unknown", domain, ConfigLiterals.domains.mkString(", "))))
    else f
  }
}
