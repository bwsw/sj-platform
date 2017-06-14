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
package com.bwsw.common.http

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{CloseableHttpResponse, HttpUriRequest}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder => ApacheHttpClientBuilder}
import org.slf4j.LoggerFactory

/**
  * Synchronous simple http client
  * Used for connecting to marathon
  *
  * @author Kseniya Tomskikh
  */
class HttpClient(timeout: Int) {
  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val requestBuilder = RequestConfig
    .custom()
    .setConnectTimeout(timeout)
    .setConnectionRequestTimeout(timeout)
    .setSocketTimeout(timeout)

  private val builder = ApacheHttpClientBuilder
    .create()
    .setDefaultRequestConfig(requestBuilder.build())

  private val client: CloseableHttpClient = {
    logger.debug("Create an http client.")
    builder.build()
  }

  def execute(request: HttpUriRequest): CloseableHttpResponse = {
    client.execute(request)
  }

  def close(): Unit = {
    logger.debug("Close an http client.")
    client.close()
  }
}

class HttpClientBuilder {
  def apply(timeout: Int): HttpClient = new HttpClient(timeout)
}
