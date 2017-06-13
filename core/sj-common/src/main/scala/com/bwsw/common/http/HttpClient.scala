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
