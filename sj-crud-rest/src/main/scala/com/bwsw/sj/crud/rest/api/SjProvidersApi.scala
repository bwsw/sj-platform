package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
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

          val validator = new ProviderValidator
          val errors = validator.validate(data)

          if (errors.isEmpty) {
            var provider = new Provider(
              data.name,
              data.description,
              data.hosts,
              data.login,
              data.password,
              data.providerType
            )
            val providerName = saveProvider(provider)

            ctx.complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, "name", s"Provider '$providerName' is created"))
            ))
          } else {
            throw new BadRecordWithKey(
              s"Cannot create provider. Errors: ${errors.mkString("\n")}",
              s"${data.name}"
            )
          }
        } ~
        get {
          val providers = providerDAO.getAll
          var msg = ""
          if (providers.nonEmpty) {
            msg = serializer.serialize(providers.map(p => providerToProviderData(p)))
          } else {
            msg = serializer.serialize(Response(200, null, s"No providers found"))
          }
          complete(HttpResponse(200, entity=HttpEntity(`application/json`, msg)))
        }
      } ~
      pathPrefix(Segment) { (providerName: String) =>
        pathEndOrSingleSlash {
          val provider = providerDAO.get(providerName)
          var msg = ""
          if (provider != null) {
            msg = serializer.serialize(providerToProviderData(provider))
          } else {
            msg = serializer.serialize(s"Provider '$providerName' not found")
          }
          complete(HttpResponse(200, entity=HttpEntity(`application/json`, msg)))
        }
      }
    }
  }

  /**
    * Save provider to db
    *
    * @param provider - provider entity
    * @return - name of saved entity
    */
  def saveProvider(provider: Provider) = {
    providerDAO.save(provider)
    provider.name
  }


  def providerToProviderData(provider: Provider) = {
    var providerData = new ProviderData(
      provider.name,
      provider.description,
      provider.login,
      provider.password,
      provider.providerType,
      provider.hosts
    )
    providerData
  }
}
