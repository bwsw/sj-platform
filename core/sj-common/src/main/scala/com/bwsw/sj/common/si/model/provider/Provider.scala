package com.bwsw.sj.common.si.model.provider

import java.net.{URI, URISyntaxException}

import com.bwsw.sj.common.dal.model.provider.{JDBCProviderDomain, ProviderDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.{normalizeName, validateName}
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.ProviderLiterals
import com.bwsw.sj.common.utils.ProviderLiterals.types

import scala.collection.mutable.ArrayBuffer

class Provider(val name: String,
               val login: String,
               val password: String,
               val providerType: String,
               val hosts: Array[String],
               val description: String) {

  def to(): ProviderDomain = {
    val provider =
      new ProviderDomain(
        name = this.name,
        description = this.description,
        hosts = this.hosts,
        login = this.login,
        password = this.password,
        providerType = this.providerType
      )

    provider
  }

  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerRepository = ConnectionRepository.getProviderRepository

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

          if (providerRepository.get(x).isDefined) {
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

object Provider {
  def from(providerDomain: ProviderDomain): Provider = {
    providerDomain.providerType match {
      case ProviderLiterals.jdbcType =>
        val jdbcProvider = providerDomain.asInstanceOf[JDBCProviderDomain]

        new JDBCProvider(
          name = jdbcProvider.name,
          login = jdbcProvider.login,
          password = jdbcProvider.password,
          hosts = jdbcProvider.hosts,
          driver = jdbcProvider.driver,
          description = jdbcProvider.description,
          providerType = jdbcProvider.providerType
        )
      case _ =>
        new Provider(
          name = providerDomain.name,
          login = providerDomain.login,
          password = providerDomain.password,
          providerType = providerDomain.providerType,
          hosts = providerDomain.hosts,
          description = providerDomain.description
        )
    }
  }
}