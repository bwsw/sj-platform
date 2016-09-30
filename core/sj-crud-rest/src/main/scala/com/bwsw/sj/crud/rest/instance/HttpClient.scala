package com.bwsw.sj.crud.rest.instance

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}

/**
  * Synchronous simple http client
  * Used for connecting to marathon
  *
  * @author Kseniya Tomskikh
  */
class HttpClient(timeout: Int) {
  private val requestBuilder = RequestConfig
      .custom()
      .setConnectTimeout(timeout)
      .setConnectionRequestTimeout(timeout)
      .setSocketTimeout(timeout)

  private val builder = HttpClientBuilder
      .create()
      .setDefaultRequestConfig(requestBuilder.build())

  val client: CloseableHttpClient = builder.build()

}
