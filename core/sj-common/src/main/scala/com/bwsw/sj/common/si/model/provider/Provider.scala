/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.si.model.provider

import java.net.{URI, URISyntaxException}
import java.util.Date

import com.bwsw.sj.common.dal.model.provider.{JDBCProviderDomain, ProviderDomain, ESProviderDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.{normalizeName, validateName}
import com.bwsw.sj.common.utils.ProviderLiterals.types
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class Provider(val name: String,
               val providerType: String,
               val hosts: Array[String],
               val description: String,
               val creationDate: String)
              (implicit injector: Injector) {

  protected val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  protected val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  private val providerRepository = connectionRepository.getProviderRepository

  def to(): ProviderDomain = {
    new ProviderDomain(
      name = this.name,
      description = this.description,
      hosts = this.hosts,
      providerType = this.providerType,
      creationDate = new Date())
  }

  /**
    * Validates provider
    *
    * @return empty array if provider is correct, validation errors otherwise
    */
  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

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
    Try(new URI(s"dummy://${normalizeName(host)}")) match {
      case Success(uri) =>
        if (Option(uri.getHost).isEmpty) {
          errors += createMessage("entity.error.wrong.host", host)
        }

        if (uri.getPort == -1) {
          errors += createMessage("entity.error.host.must.contains.port", host)
        }

        val path = uri.getPath

        if (path.length > 0)
          errors += createMessage("entity.error.host.should.not.contain.uri", path)

      case Failure(_: URISyntaxException) =>
        errors += createMessage("entity.error.wrong.host", host)

      case Failure(e) => throw e
    }

    errors
  }
}

class ProviderCreator {
  def from(providerDomain: ProviderDomain)(implicit injector: Injector): Provider = {
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
          providerType = jdbcProvider.providerType,
          creationDate = jdbcProvider.creationDate.toString)

      case ProviderLiterals.elasticsearchType =>
        val esProvider = providerDomain.asInstanceOf[ESProviderDomain]

        new ESProvider(
          name = esProvider.name,
          login = esProvider.login,
          password = esProvider.password,
          providerType = esProvider.providerType,
          hosts = esProvider.hosts,
          description = esProvider.description,
          creationDate = esProvider.creationDate.toString)

      case _ =>
        new Provider(
          name = providerDomain.name,
          providerType = providerDomain.providerType,
          hosts = providerDomain.hosts,
          description = providerDomain.description,
          creationDate = providerDomain.creationDate.toString)
    }
  }
}