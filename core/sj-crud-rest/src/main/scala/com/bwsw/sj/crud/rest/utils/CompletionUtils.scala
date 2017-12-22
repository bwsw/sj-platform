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
package com.bwsw.sj.crud.rest.utils

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, MediaTypes}
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.rest.RestResponse
import com.bwsw.sj.crud.rest.{CustomFile, CustomJar, ModuleJar}

/**
  * Provides methods for completion of sj-api response
  */
trait CompletionUtils {
  private val responseSerializer = new JsonSerializer()

  def restResponseToHttpResponse(restResponse: RestResponse): HttpResponse = {
    restResponse match {
      case customJar: CustomJar =>
        HttpResponse(
          headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> customJar.filename))),
          entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, customJar.source)
        )

      case customFile: CustomFile =>
        HttpResponse(
          headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> customFile.filename))),
          entity = HttpEntity.Chunked.fromData(ContentTypes.`application/octet-stream`, customFile.source)
        )

      case moduleJar: ModuleJar =>
        HttpResponse(
          headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> moduleJar.filename))),
          entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, moduleJar.source)
        )

      case _ =>
        HttpResponse(
          status = restResponse.statusCode,
          entity = HttpEntity(`application/json`, responseSerializer.serialize(restResponse))
        )
    }
  }
}
