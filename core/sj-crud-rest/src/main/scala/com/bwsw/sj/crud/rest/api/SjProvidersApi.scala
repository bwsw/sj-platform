package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.{BadRecordWithKey, NotFoundException}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.provider.ProviderData
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
            val response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
              messages.getString("rest.providers.provider.created"),
              provider.name
            )))
            ctx.complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } else {
            throw new BadRecordWithKey(
              MessageFormat.format(
                messages.getString("rest.providers.provider.cannot.create"),
                errors.mkString("\n")
              ),
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
            response = ProtocolResponse(200, Map("message" -> messages.getString("rest.providers.notfound")))
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
              response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                messages.getString("rest.providers.provider.notfound"),
                providerName
              )))
            }
            complete(HttpEntity(`application/json`, serializer.serialize(response)))
          } ~
          delete {
            val providers = serviceDAO.getAll.filter {
              case esService: ESService =>
                esService.provider.name.equals(providerName)
              case zkService: ZKService =>
                zkService.provider.name.equals(providerName)
              case aeroService: AerospikeService =>
                aeroService.provider.name.equals(providerName)
              case cassService: CassandraService =>
                cassService.provider.name.equals(providerName)
              case kfkService: KafkaService =>
                kfkService.provider.name.equals(providerName)
              case tService: TStreamService =>
                tService.metadataProvider.name.equals(providerName) || tService.dataProvider.name.equals(providerName)
              case _ =>
                false //todo redis and jdbc
            }
            if (providers.isEmpty) {
              val provider = providerDAO.get(providerName)
              var response: ProtocolResponse = null
              if (provider != null) {
                providerDAO.delete(providerName)
                response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                  messages.getString("rest.providers.provider.deleted"),
                  providerName
                )))
              } else {
                response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                  messages.getString("rest.providers.provider.notfound"),
                  providerName
                )))
              }
              complete(HttpEntity(`application/json`, serializer.serialize(response)))
            } else {
              throw new BadRecordWithKey(MessageFormat.format(
                messages.getString("rest.providers.provider.cannot.delete"),
                providerName
              ), providerName)
            }
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
                  MessageFormat.format(
                    messages.getString("rest.providers.provider.notfound"),
                    providerName),
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
