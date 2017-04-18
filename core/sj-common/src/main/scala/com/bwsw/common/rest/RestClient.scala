package com.bwsw.common.rest

import java.net.URI
import java.util.concurrent.TimeUnit

import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.{BasicAuthentication, StringContentProvider}
import org.eclipse.jetty.http.{HttpMethod, HttpVersion}
import org.slf4j.LoggerFactory

/**
  * Client for RESTful service.
  *
  * @param hosts       set of services hosts
  * @param path        path to entities
  * @param httpVersion version of HTTP
  * @param headers     set of headers
  * @param username
  * @param password
  * @author Pavel Tomskikh
  */
class RestClient(
    hosts: Set[String],
    path: String,
    httpVersion: HttpVersion,
    headers: Map[String, String],
    username: String = null,
    password: String = null) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val urls = hosts.map { host => new URI("http://" + host) }
  private val client = new HttpClient
  if (username != null && username.nonEmpty && password != null && password.nonEmpty)
    urls.foreach(addAuthentication)

  client.start()

  def post(data: String, contentType: String): Boolean = {
    logger.debug(s"Post '$data' with contentType '$contentType' to REST service")
    trySendRequest(_
      .method(HttpMethod.POST)
      .content(new StringContentProvider(data), contentType))
  }

  def delete(field: String, value: String): Boolean = {
    logger.debug(s"Delete entity with '$field' = '$value' fro REST service")
    trySendRequest(_
      .method(HttpMethod.DELETE)
      .param(field, value))
  }

  def close(): Unit = client.stop()

  private def addAuthentication(url: URI): Unit = {
    val authenticationStore = client.getAuthenticationStore
    authenticationStore.addAuthenticationResult(
      new BasicAuthentication.BasicResult(url, username, password))
  }

  private def trySendRequest(requestModification: Request => Request): Boolean = {
    urls.exists { url =>
      val request = client.newRequest(url)
        .version(httpVersion)
        .timeout(ConfigurationSettingsUtils.getRestTimeout, TimeUnit.MILLISECONDS)
        .path(path)
      headers.foreach { header => request.header(header._1, header._2) }

      try {
        val response = requestModification(request).send()
        val status = response.getStatus
        status >= 200 && status < 300
      } catch {
        case _: Throwable => false
      }
    }
  }
}
