package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.{BadRecordWithKey, NotFoundException}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.utils.ConvertUtil.providerToProviderData
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.provider.ProviderValidator

/**
  * Rest-api for streams
  *
  * Created by mendelbaum_nm
  */
trait SjProvidersApi extends Directives with SjCrudValidator {

  val providersApi = {
    pathPrefix("providers") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val data = serializer.deserialize[ProviderData](getEntityFromContext(ctx))

          val errors = ProviderValidator.validate(data)

          if (errors.isEmpty) {
            val provider = new Provider(
              data.name,
              data.description,
              data.hosts,
              data.login,
              data.password,
              data.providerType
            )
            providerDAO.save(provider)
            val response = ProtocolResponse(200, Map("message" -> s"Provider '${provider.name}' is created"))
            ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } else {
            throw new BadRecordWithKey(
              s"Cannot create provider. Errors: ${errors.mkString("\n")}",
              s"${data.name}"
            )
          }
        } ~
        get {
          val providers = providerDAO.getAll
          var response: ProtocolResponse = null
          if (providers.nonEmpty) {
            val entity = Map("providers" -> providers.map(p => providerToProviderData(p)))
            response = ProtocolResponse(200, entity)
          } else {
            response = ProtocolResponse(200, Map("message" -> "No providers found"))
          }
          complete(HttpEntity(`application/json`, serializer.serialize(response)))
        }
      } ~
      pathPrefix(Segment) { (providerName: String) =>
        pathEndOrSingleSlash {
          get {
            val provider = providerDAO.get(providerName)
            var response: ProtocolResponse = null
            if (provider != null) {
              val entity = Map("providers" -> providerToProviderData(provider))
              response = ProtocolResponse(200, entity)
            } else {
              response = ProtocolResponse(200, Map("message" -> s"Provider '$providerName' not found"))
            }
            complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } ~
          delete {
            val provider = providerDAO.get(providerName)
            var response: ProtocolResponse = null
            if (provider != null) {
              providerDAO.delete(providerName)
              response = ProtocolResponse(200, Map("message" -> s"Provider '$providerName' has been deleted"))
            } else {
              response = ProtocolResponse(200, Map("message" -> s"Provider '$providerName' not found"))
            }
            complete(HttpEntity(`application/json`, serializer.serialize(response)))
          }
        } ~
        pathPrefix("connection") {
          pathEndOrSingleSlash {
            get {
              val provider = providerDAO.get(providerName)
              var response: ProtocolResponse = null
              if (provider != null) {
                val errors = ProviderValidator.checkProviderConnection(provider)
                if (errors.isEmpty) {
                  response = ProtocolResponse(200, Map("connection" -> true))
                }
                else {
                  response = ProtocolResponse(200, Map(
                    "connection" -> false,
                    "errors" -> errors.mkString("\n")
                  ))
                }
                complete(HttpEntity(`application/json`, serializer.serialize(response)))
              }
              else {
                throw new NotFoundException(
                  s"Provider not found.",
                  providerName
                )
              }
            }
          }
        }
      }
    }
  }
}
