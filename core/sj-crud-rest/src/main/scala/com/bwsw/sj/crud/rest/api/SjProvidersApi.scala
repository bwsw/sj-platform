package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.provider.ProviderData
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable

trait SjProvidersApi extends Directives with SjCrudValidator with CompletionUtils {

  val providersApi = {
    pathPrefix("providers") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          checkContext(ctx)
          val data = serializer.deserialize[ProviderData](getEntityFromContext(ctx))
          val errors = data.validate()
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            createMessage("rest.providers.provider.cannot.create", errors.mkString(";"))))

          if (errors.isEmpty) {
            providerDAO.save(data.asModelProvider())
            response = CreatedRestResponse(Map("message" ->
              createMessage("rest.providers.provider.created", data.name)))
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val providers = providerDAO.getAll
            val response = OkRestResponse(Map("providers" -> mutable.Buffer()))
            if (providers.nonEmpty) {
              response.entity = Map("providers" -> providers.map(p => p.asProtocolProvider()))
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix(Segment) { (providerName: String) =>
          pathEndOrSingleSlash {
            get {
              val provider = providerDAO.get(providerName)
              var response: RestResponse = NotFoundRestResponse(Map("message" ->
                createMessage("rest.providers.provider.notfound", providerName)))
              provider match {
                case Some(x) =>
                  val entity = Map("providers" -> x.asProtocolProvider())
                  response = OkRestResponse(entity)
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                  createMessage("rest.providers.provider.cannot.delete", providerName)))
                val providers = getUsedProviders(providerName)
                if (providers.isEmpty) {
                  val provider = providerDAO.get(providerName)
                  provider match {
                    case Some(_) =>
                      providerDAO.delete(providerName)
                      response = OkRestResponse(Map("message" ->
                        createMessage("rest.providers.provider.deleted", providerName)))
                    case None =>
                      response = NotFoundRestResponse(Map("message" ->
                        createMessage("rest.providers.provider.notfound", providerName)))
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
                    Map("message" -> createMessage("rest.providers.provider.notfound", providerName)))

                  provider match {
                    case Some(x) =>
                      val errors = x.checkConnection()
                      if (errors.isEmpty) {
                        response = OkRestResponse(Map("connection" -> true))
                      }
                      else {
                        response = ConflictRestResponse(Map(
                          "connection" -> false,
                          "errors" -> errors.mkString(";")
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

  private def getUsedProviders(providerName: String) = {
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
      case jdbcService: JDBCService =>
        jdbcService.provider.name.equals(providerName)
    }
  }
}
