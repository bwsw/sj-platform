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
package com.bwsw.common.rest

import java.net.URI
import java.util.concurrent.TimeUnit

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.http.{HttpScheme, HttpStatus, HttpVersion}
import org.eclipse.jetty.util.ssl.SslContextFactory

import scala.util.{Failure, Success, Try}

/**
  * Client for [[com.bwsw.sj.common.utils.EngineLiterals.outputStreamingType]] that has got
  * [[com.bwsw.sj.common.utils.StreamLiterals.restOutputType]] output.
  *
  * @param hosts       set of services hosts
  * @param path        path to entities
  * @param httpVersion version of HTTP
  * @param headers     set of headers
  * @param timeout     the total timeout for the request/response conversation (in milliseconds)
  * @author Pavel Tomskikh
  */
class RestClient(hosts: Set[String],
                 path: String,
                 httpScheme: HttpScheme,
                 httpVersion: HttpVersion,
                 headers: Map[String, String],
                 timeout: Long = 5000) {

  private val urls = hosts.map { host => new URI(s"${httpScheme.toString}://" + host) }
  private val client = new HttpClient(new SslContextFactory(true))
  client.start()


  def execute(requestModification: Request => Request): Boolean = {
    urls.exists { url =>
      val request = client.newRequest(url)
        .version(httpVersion)
        .timeout(timeout, TimeUnit.MILLISECONDS)
        .path(path)
      headers.foreach { header => request.header(header._1, header._2) }

      Try {
        val response = requestModification(request).send()

        response.getStatus
      } match {
        case Success(status) => status >= HttpStatus.OK_200 && status < HttpStatus.MULTIPLE_CHOICES_300
        case Failure(_) => false
      }
    }
  }

  def close(): Unit = client.stop()
}
