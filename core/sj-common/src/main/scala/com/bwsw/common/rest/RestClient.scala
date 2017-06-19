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

import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.BasicAuthentication
import org.eclipse.jetty.http.{HttpStatus, HttpVersion}

import scala.util.{Failure, Success, Try}


/**
  * Client for [[EngineLiterals.outputStreamingType]] that has got [[StreamLiterals.restOutputType]] output.
  *
  * @param hosts       set of services hosts
  * @param path        path to entities
  * @param httpVersion version of HTTP
  * @param headers     set of headers
  * @param timeout     the total timeout for the request/response conversation (in milliseconds)
  * @param username    username to authenticate
  * @param password    password to authenticate
  * @author Pavel Tomskikh
  */
class RestClient(hosts: Set[String],
                 path: String,
                 httpVersion: HttpVersion,
                 headers: Map[String, String],
                 username: Option[String] = None,
                 password: Option[String] = None,
                 timeout: Long = 5000) {

  private val urls = hosts.map { host => new URI("http://" + host) }
  private val client = new HttpClient
  setAuthenticationParameters()
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

  private def setAuthenticationParameters() = {
    if (username.getOrElse("").nonEmpty && password.getOrElse("").nonEmpty)
      urls.foreach(addAuthentication)
  }

  private def addAuthentication(url: URI): Unit = {
    val authenticationStore = client.getAuthenticationStore
    authenticationStore.addAuthenticationResult(
      new BasicAuthentication.BasicResult(url, username.get, password.get))
  }
}
