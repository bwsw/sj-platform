package com.bwsw.sj.crud.rest.controller

import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.si.ConfigSettingsSI
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.crud.rest.model.config.ConfigurationSettingApi
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.exceptions.UnknownConfigSettingDomain

import scala.util.{Failure, Success, Try}

class ConfigSettingsController extends Controller {
  val serviceInterface = new ConfigSettingsSI()

  def get(name: String): RestResponse = {
    val configSetting = serviceInterface.get(name)

    val response = configSetting match {
      case Some(x) =>
        OkRestResponse(ConfigSettingResponseEntity(ConfigurationSettingApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.config.setting.notfound", name)))
    }

    response
  }

  def getAll(): RestResponse = {
    val response = OkRestResponse(ConfigSettingsResponseEntity())
    val configElements = serviceInterface.getAll()
    if (configElements.nonEmpty) {
      response.entity = ConfigSettingsResponseEntity(configElements.map(cs => ConfigurationSettingApi.from(cs)))
    }

    response
  }

  def create(serializedEntity: String): RestResponse = {
    var response: RestResponse = new RestResponse()

    val triedConfigData = Try(serializer.deserialize[ConfigurationSettingApi](serializedEntity))
    triedConfigData match  {
      case Success(configData) =>
        val created = serviceInterface.create(configData.to())
        response = created match {
          case Right(_) =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.config.setting.created", configData.name)))
          case Left(errors) => BadRequestRestResponse(MessageResponseEntity(
            createMessage("rest.config.setting.cannot.create", errors.mkString(";"))
          ))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = JsonDeserializationErrorMessageCreator(exception)
        response = BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.config.setting.cannot.create", error)))

      case Failure(exception) => throw exception
    }

    response
  }

  def delete(name: String): RestResponse = {
    val deleteResponse = serviceInterface.delete(name)
    val response: RestResponse = deleteResponse match {
      case Right(isDeleted) =>
        if (isDeleted)
          OkRestResponse(MessageResponseEntity(createMessage("rest.config.setting.deleted", name)))
        else
          NotFoundRestResponse(MessageResponseEntity(createMessage("rest.config.setting.notfound", name)))
      case Left(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }

    response
  }

  def getDomain(domain: String): RestResponse = {
    val configElements = serviceInterface.getDomain(Map("domain" -> domain))
    val response = OkRestResponse(ConfigSettingsResponseEntity())
    if (configElements.nonEmpty) {
      response.entity = ConfigSettingsResponseEntity(configElements.map(cs => ConfigurationSettingApi.from(cs)))
    }

    response
  }

  def checkDomain(domain: String): Unit = {
    if (!ConfigLiterals.domains.contains(domain))
      throw UnknownConfigSettingDomain(
        createMessage("rest.config.setting.domain.unknown", ConfigLiterals.domains.mkString(", ")),
        domain
      )
  }
}
