package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ConfigSettingsSI
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.config.ConfigurationSettingApi
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ConfigSettingsController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  val serviceInterface = inject[ConfigSettingsSI]

  override protected val entityDeletedMessage: String = "rest.config.setting.deleted"
  override protected val entityNotFoundMessage: String = "rest.config.setting.notfound"

  def get(name: String): RestResponse = {
    val configSetting = serviceInterface.get(name)

    val response = configSetting match {
      case Some(x) =>
        OkRestResponse(ConfigSettingResponseEntity(ConfigurationSettingApi.from(x)))
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }

    response
  }

  def get(domain: String, name: String): RestResponse =
    ifDomainCorrect(domain)(get(ConfigurationSetting.createConfigurationSettingName(domain, name)))

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

    val triedSettingApi = Try(serializer.deserialize[ConfigurationSettingApi](serializedEntity))
    triedSettingApi match {
      case Success(configData) =>
        val created = serviceInterface.create(configData.to())
        response = created match {
          case Created =>
            CreatedRestResponse(MessageResponseEntity(createMessage("rest.config.setting.created", configData.name)))
          case NotCreated(errors) => BadRequestRestResponse(MessageResponseEntity(
            createMessageWithErrors("rest.config.setting.cannot.create", errors)
          ))
        }
      case Failure(exception: JsonDeserializationException) =>
        val error = jsonDeserializationErrorMessageCreator(exception)
        response = BadRequestRestResponse(MessageResponseEntity(
          createMessage("rest.config.setting.cannot.create", error)))

      case Failure(exception) => throw exception
    }

    response
  }

  def getByDomain(domain: String): RestResponse = {
    ifDomainCorrect(domain) {
      val configElements = serviceInterface.getBy(domain)
      val response = OkRestResponse(ConfigSettingsResponseEntity())
      if (configElements.nonEmpty) {
        response.entity = ConfigSettingsResponseEntity(configElements.map(cs => ConfigurationSettingApi.from(cs)))
      }

      response
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
