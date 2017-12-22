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
package com.bwsw.sj.crud.rest.cors

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import com.bwsw.sj.crud.rest.utils.RestLiterals
import com.typesafe.config.ConfigFactory

/**
  * Trait from akka-http-rest template https://github.com/ArchDev/akka-http-rest
  */
trait CorsSupport {
  lazy val allowedOriginHeader = {
    val config = ConfigFactory.load()
    val sAllowedOrigin = config.getString(RestLiterals.corsAllowedOriginConfig)
    if (sAllowedOrigin == "*")
      `Access-Control-Allow-Origin`.*
    else
      `Access-Control-Allow-Origin`(HttpOrigin(sAllowedOrigin))
  }

  private def addAccessControlHeaders(): Directive0 = {
    mapResponseHeaders { headers =>
      allowedOriginHeader +:
        `Access-Control-Allow-Credentials`(true) +:
        `Access-Control-Allow-Headers`("Token", "Content-Type", "X-Requested-With") +:
        headers
    }
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(200).withHeaders(
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)
    )
    )
  }

  def corsHandler(r: Route) = addAccessControlHeaders() {
    preflightRequestHandler ~ r
  }
}
