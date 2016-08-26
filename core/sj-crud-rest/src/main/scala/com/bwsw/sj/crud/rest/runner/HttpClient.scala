package com.bwsw.sj.crud.rest.runner

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}

/**
  * Synchronous http client
  *
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
