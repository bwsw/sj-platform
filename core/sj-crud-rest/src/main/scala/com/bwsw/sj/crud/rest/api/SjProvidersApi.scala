package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.provider.ProviderData
import com.bwsw.sj.common.utils.ProviderLiterals
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjProvidersApi extends Directives with SjCrudValidator {

  val providersApi = {
    pathPrefix("providers") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          validateContextWithSchema(ctx, "providerSchema.json")
          val data = serializer.deserialize[ProviderData](getEntityFromContext(ctx))
          val errors = data.validate()
          var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
            createMessage("rest.providers.provider.cannot.create", errors.mkString(";"))
          ))

          if (errors.isEmpty) {
            providerDAO.save(data.asModelProvider())
            response = CreatedRestResponse(MessageResponseEntity(createMessage("rest.providers.provider.created", data.name)))
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val providers = providerDAO.getAll
            val response = OkRestResponse(ProvidersResponseEntity())
            if (providers.nonEmpty) {
              response.entity = ProvidersResponseEntity(providers.map(p => p.asProtocolProvider()))
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(TypesResponseEntity(ProviderLiterals.types))

              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (providerName: String) =>
          pathEndOrSingleSlash {
            get {
              val provider = providerDAO.get(providerName)
              var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
                createMessage("rest.providers.provider.notfound", providerName)))
              provider match {
                case Some(x) =>
                  response = OkRestResponse(ProviderResponseEntity(x.asProtocolProvider()))
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
                  createMessage("rest.providers.provider.cannot.delete", providerName)))
                val providers = getRelatedServices(providerName)
                if (providers.isEmpty) {
                  val provider = providerDAO.get(providerName)
                  provider match {
                    case Some(_) =>
                      providerDAO.delete(providerName)
                      response = OkRestResponse(MessageResponseEntity(
                        createMessage("rest.providers.provider.deleted", providerName)))
                    case None =>
                      response = NotFoundRestResponse(MessageResponseEntity(
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
                    MessageResponseEntity(createMessage("rest.providers.provider.notfound", providerName)))

                  provider match {
                    case Some(x) =>
                      val errors = x.checkConnection()
                      if (errors.isEmpty) {
                        response = OkRestResponse(new ConnectionResponseEntity())
                      }
                      else {
                        response = ConflictRestResponse(TestConnectionResponseEntity(connection = false, errors.mkString(";")))
                      }
                    case None =>
                  }

                  complete(restResponseToHttpResponse(response))
                }
              }
            } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  val provider = providerDAO.get(providerName)
                  var response: RestResponse = NotFoundRestResponse(
                    MessageResponseEntity(createMessage("rest.providers.provider.notfound", providerName)))

                  provider match {
                    case Some(x) =>
                      response = OkRestResponse(RelatedToProviderResponseEntity(getRelatedServices(providerName)))
                    case None =>
                  }

                  complete(restResponseToHttpResponse(response))
                }
              }
            }
        }
    }
  }

  private def getRelatedServices(providerName: String) = {
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
    }.map(_.name)
  }
}
