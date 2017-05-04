package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.rest.{OkRestResponse, TypesResponseEntity}
import com.bwsw.sj.common.utils.ProviderLiterals
import com.bwsw.sj.crud.rest.BLL.ProviderLogic
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjProvidersApi extends Directives with SjCrudValidator {
  private val providerLogic = new ProviderLogic()

  val providersApi = {
    pathPrefix("providers") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val entity = getEntityFromContext(ctx)
          val response = providerLogic.process(entity)

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val response = providerLogic.getAll()

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
              val response = providerLogic.get(providerName)

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                val response = providerLogic.delete(providerName)

                complete(restResponseToHttpResponse(response))
              }
          } ~
            pathPrefix("connection") {
              pathEndOrSingleSlash {
                get {
                  val response = providerLogic.checkConnection(providerName)

                  complete(restResponseToHttpResponse(response))
                }
              }
            } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  val response = providerLogic.getRelated(providerName)

                  complete(restResponseToHttpResponse(response))
                }
              }
            }
        }
    }
  }
}
