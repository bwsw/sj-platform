package com.bwsw.sj.crud.rest.instance

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
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

  private val builder = HttpClientBuilder
      .create()
      .setDefaultRequestConfig(requestBuilder.build())

  val client: CloseableHttpClient = {
    logger.debug("Create an http client.")
    builder.build()
  }

  def close(): Unit = {
    logger.debug("Close an http client.")
    client.close()
  }
}
