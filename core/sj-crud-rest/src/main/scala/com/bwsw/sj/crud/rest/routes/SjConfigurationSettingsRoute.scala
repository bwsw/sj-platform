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
package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext, Route}
import com.bwsw.sj.common.SjInjector
import com.bwsw.sj.common.rest._
import com.bwsw.sj.crud.rest.SjCrudRestServer
import com.bwsw.sj.crud.rest.controller.ConfigSettingsController
import com.bwsw.sj.crud.rest.utils.CompletionUtils

trait SjConfigurationSettingsRoute extends Directives with SjCrudRestServer with CompletionUtils with SjInjector {
  val configSettingsController = new ConfigSettingsController

  val configSettingsRoute: Route = {
    pathPrefix("config") {
      pathPrefix("settings") {
        pathPrefix("domains") {
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(DomainsResponseEntity())
              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
          pathPrefix(Segment) { (domain: String) =>
            pathEndOrSingleSlash {
              get {
                val response = configSettingsController.getByDomain(domain)
                complete(restResponseToHttpResponse(response))
              }
            } ~
              pathPrefix(Segment) { (name: String) =>
                pathEndOrSingleSlash {
                  get {
                    val response = configSettingsController.get(domain, name)
                    complete(restResponseToHttpResponse(response))
                  } ~
                    delete {
                      val response = configSettingsController.delete(domain, name)
                      complete(restResponseToHttpResponse(response))
                    }
                }
              }
          } ~
          pathEndOrSingleSlash {
            post { (ctx: RequestContext) =>
              val entity = getEntityFromContext(ctx)
              val response = configSettingsController.create(entity)
              ctx.complete(restResponseToHttpResponse(response))
            } ~
              get {
                val response = configSettingsController.getAll()
                complete(restResponseToHttpResponse(response))
              }
          }
      }
    }
  }
}
