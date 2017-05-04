package com.bwsw.sj.common.rest.entities.provider

import java.net.{URI, URISyntaxException}

import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.Data
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.ProviderLiterals._
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}

import scala.collection.mutable.ArrayBuffer

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = classOf[ProviderData], visible = true)
@JsonSubTypes(Array(
  new Type(value = classOf[JDBCProviderData], name = ProviderLiterals.jdbcType)
))
class ProviderData(val name: String,
                   val login: String,
                   val password: String,
                   @JsonProperty("type") val providerType: String,
                   val hosts: Array[String],
                   val description: String = "No description"
                  ) extends Data with ValidationUtils with MessageResourceUtils {
  @JsonIgnore
  def asModelProvider(): Provider = {
    val provider = new Provider(
      this.name,
      this.description,
      this.hosts,
      this.login,
      this.password,
      this.providerType
    )

    provider
  }

  @JsonIgnore
  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Provider", x, "provider")
          }

          if (providerDAO.get(x).isDefined) {
            errors += createMessage("entity.error.already.exists", "Provider", x)
          }
        }
    }

    // 'providerType field
    Option(this.providerType) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Type")
        }
        else {
          if (!types.contains(x)) {
            errors += createMessage("entity.error.unknown.type.must.one.of", x, "provider", types.mkString("[", ", ", "]"))
          }
        }
    }

    //'hosts' field
    Option(this.hosts) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Hosts")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.hosts.should.be.non.empty")
        } else {
          if (x.head.isEmpty) {
            errors += createMessage("entity.error.attribute.required", "Hosts")
          }
          else {
            for (host <- this.hosts) {
              errors ++= validateHost(host)
            }
          }
        }
    }

    errors
  }

  private def validateHost(host: String): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    try {
      val uri = new URI(s"dummy://${normalizeName(host)}")
      val hostname = uri.getHost

      if (hostname == null) {
        errors += createMessage("entity.error.wrong.host", host)
      }

      if (uri.getPort == -1) {
        errors += createMessage("entity.error.host.must.contains.port", host)
      }

      val path = uri.getPath

      if (path.length > 0)
        errors += createMessage("entity.error.host.should.not.contain.uri", path)

    } catch {
      case ex: URISyntaxException =>
        errors += createMessage("entity.error.wrong.host", host)
    }

    errors
  }
}