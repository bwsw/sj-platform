package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.provider.ProviderData
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.bwsw.sj.crud.rest.utils.ConvertUtil.providerToProviderData
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.provider.ProviderValidator

/**
 * Rest-api for streams
 *
 * Created by mendelbaum_nm
 */
trait SjProvidersApi extends Directives with SjCrudValidator with CompletionUtils {

  val providersApi = {
    pathPrefix("providers") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val data = serializer.deserialize[ProviderData](getEntityFromContext(ctx))
          val errors = ProviderValidator.validate(data)
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            MessageFormat.format(messages.getString("rest.providers.provider.cannot.create"), errors.mkString("\n")))
          )

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
            response = CreatedRestResponse(Map("message" ->
              MessageFormat.format(messages.getString("rest.providers.provider.created"), provider.name))
            )
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val providers = providerDAO.getAll
            var response: RestResponse = NotFoundRestResponse(Map("message" -> messages.getString("rest.providers.notfound")))
            if (providers.nonEmpty) {
              val entity = Map("providers" -> providers.map(p => providerToProviderData(p)))
              response = OkRestResponse(entity)
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix(Segment) { (providerName: String) =>
          pathEndOrSingleSlash {
            get {
              val provider = providerDAO.get(providerName)
              var response: RestResponse = NotFoundRestResponse(Map("message" ->
                MessageFormat.format(messages.getString("rest.providers.provider.notfound"), providerName))
              )
              provider match {
                case Some(x) =>
                  val entity = Map("providers" -> providerToProviderData(x))
                  response = OkRestResponse(entity)
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                  MessageFormat.format(messages.getString("rest.providers.provider.cannot.delete"), providerName)))
                val providers = getUsedProviders(providerName)
                if (providers.isEmpty) {
                  val provider = providerDAO.get(providerName)
                  provider match {
                    case Some(_) =>
                      providerDAO.delete(providerName)
                      response = OkRestResponse(Map("message" ->
                        MessageFormat.format(messages.getString("rest.providers.provider.deleted"), providerName))
                      )
                    case None =>
                      response = NotFoundRestResponse(Map("message" ->
                        MessageFormat.format(messages.getString("rest.providers.provider.notfound"), providerName))
                      )
                  }
                }

                complete(restResponseToHttpResponse(response))
              }
          } ~
            pathPrefix("connection") {
              pathEndOrSingleSlash {
                get {
                  val provider = providerDAO.get(providerName)
                  var response: RestResponse = NotFoundRestResponse(
                    Map("message" -> MessageFormat.format(messages.getString("rest.providers.provider.notfound"), providerName)))

                  provider match {
                    case Some(x) =>
                      val errors = ProviderValidator.checkProviderConnection(x)
                      if (errors.isEmpty) {
                        response = OkRestResponse(Map("connection" -> true))
                      }
                      else {
                        response = ConflictRestResponse(Map(
                          "connection" -> false,
                          "errors" -> errors.mkString("\n")
                        ))
                      }
                    case None =>
                  }

                  complete(restResponseToHttpResponse(response))
                }
              }
            }
        }
    }
  }

  def getUsedProviders(providerName: String) = {
    serviceDAO.getAll.filter {
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
  }
}
