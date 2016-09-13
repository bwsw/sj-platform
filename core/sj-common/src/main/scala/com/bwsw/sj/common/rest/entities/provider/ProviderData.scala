package com.bwsw.sj.common.rest.entities.provider

import java.net.{URI, URISyntaxException}

import com.bwsw.sj.common.DAL.model.Provider
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.Provider._
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ArrayBuffer

case class ProviderData(name: String,
                        login: String,
                        password: String,
                        @JsonProperty("type") providerType: String,
                        hosts: Array[String],
                        description: String = "No description"
                         ) extends ValidationUtils {
  def asModelProvider() = {
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

  def validate() = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += s"'Name' is required"
      case Some(x) =>
        if (providerDAO.get(x).isDefined) {
          errors += s"Provider with name '$x' already exists"
        }

        if (!validateName(x)) {
          errors += s"Provider has incorrect name: '$x'. " +
            s"Name of provider must be contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    // 'providerType field
    Option(this.providerType) match {
      case None =>
        errors += s"'Type' is required"
      case Some(x) =>
        if (!providerTypes.contains(x)) {
          errors += s"Unknown type '$x' provided. Must be one of: ${providerTypes.mkString("[", ", ", "]")}"
        }
    }

    //'hosts' field
    if (Option(this.hosts).isEmpty) {
      errors += s"'Hosts' is required"
    } else {
      if (this.hosts.isEmpty) {
        errors += s"'Hosts' must contain at least one host"
      } else {
        for (host <- this.hosts) {
          errors ++= validateHost(host)
        }
      }
    }

    errors
  }

  private def validateHost(host: String): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    try {
      val uri = new URI(s"dummy://$host")
      val hostname = uri.getHost

      if (hostname == null) {
        errors += s"Wrong host provided: '$host'"
      }

      val path = uri.getPath

      if (path.length > 0)
        errors += s"Host cannot contain any uri path ('$path')"

    } catch {
      case ex: URISyntaxException =>
        errors += s"Wrong host provided: '$host'"
    }

    errors
  }
}
