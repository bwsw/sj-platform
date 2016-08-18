package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.crud.rest.entities.ProtocolResponse
import com.bwsw.sj.crud.rest.entities.config.ConfigSettingData
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.config.ConfigSettingValidator
import com.bwsw.sj.crud.rest.utils.ConvertUtil._

/**
 * Rest-api for config file
 *
 */
trait SjConfigSettingsApi extends Directives with SjCrudValidator {

  val configSettingsApi = {
    pathPrefix("config") {
      pathPrefix("settings") {
        pathPrefix(Segment) { (domain: String) =>
          if (!ConfigConstants.domains.contains(domain)) throw new BadRecordWithKey(
            s"Cannot recognize config setting domain. Domain must be one of the following values: ${ConfigConstants.domains.mkString(", ")}",
            s"$domain"
          )
          pathEndOrSingleSlash {
            post { (ctx: RequestContext) =>
              val data = serializer.deserialize[ConfigSettingData](getEntityFromContext(ctx))

              val errors = ConfigSettingValidator.validate(data)

              if (errors.isEmpty) {
                val configElement = new ConfigSetting(
                  domain + "." + data.name,
                  data.value,
                  domain
                )
                configService.save(configElement)
                val response = ProtocolResponse(200, Map("message" -> s"$domain config setting '${data.name}' is created"))
                ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
              } else {
                throw new BadRecordWithKey(
                  s"Cannot create $domain config setting. Errors: ${errors.mkString("\n")}",
                  s"${data.name}"
                )
              }
            } ~
              get {
                val configElements = configService.getByParameters(Map("domain" -> domain))
                var response: Option[ProtocolResponse] = None
                if (configElements.nonEmpty) {
                  val entity = Map(s"$domain-config-settings" -> configElements.map(x => configSettingToConfigSettingData(x)))
                  response = Some(ProtocolResponse(200, entity))
                } else {
                  response = Some(ProtocolResponse(200, Map("message" -> ("There are no " + domain + " config settings"))))
                }
                response match {
                  case None => throw new Exception("Something was going seriously wrong")
                  case Some(x) => complete(HttpEntity(`application/json`, serializer.serialize(x)))
                }
              }
          } ~
            pathPrefix(Segment) { (name: String) =>
              pathEndOrSingleSlash {
                get {
                  val configElement = configService.get(domain + "." + name)
                  var response: Option[ProtocolResponse] = None
                  if (configElement != null) {
                    val entity = Map(s"$domain-config-settings" -> configSettingToConfigSettingData(configElement))
                    response = Some(ProtocolResponse(200, entity))
                  } else {
                    response = Some(ProtocolResponse(200, Map("message" -> s"$domain config setting '$name' has not found")))
                  }
                  response match {
                    case None => throw new Exception("Something was going seriously wrong")
                    case Some(x) => complete(HttpEntity(`application/json`, serializer.serialize(x)))
                  }
                } ~
                  delete {
                    var response: Option[ProtocolResponse] = None
                    if (configService.get(domain + "." + name) != null) {
                      val entity = Map("message" -> s"$domain config setting '$name' has been deleted")
                      response = Some(ProtocolResponse(200, entity))
                      configService.delete(domain + "." + name)
                    } else {
                      response = Some(ProtocolResponse(200, Map("message" -> s"$domain config setting '$name' has not found")))
                    }
                    response match {
                      case None => throw new Exception("Something was going seriously wrong")
                      case Some(x) => complete(HttpEntity(`application/json`, serializer.serialize(x)))
                    }
                  }
              }
            }
        } ~ pathEndOrSingleSlash {
          get {
            val configElements = configService.getAll
            var response: Option[ProtocolResponse] = None
            if (configElements.nonEmpty) {
              val entity = Map("config-settings" -> configElements.map(x => (x.domain, configSettingToConfigSettingData(x))).toMap)
              response = Some(ProtocolResponse(200, entity))
            } else {
              response = Some(ProtocolResponse(200, Map("message" -> "There are no config settings")))
            }
            response match {
              case None => throw new Exception("Something was going seriously wrong")
              case Some(x) => complete(HttpEntity(`application/json`, serializer.serialize(x)))
            }
          }
        }
      }
    }
  }
}

