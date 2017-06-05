package com.bwsw.common.rest

import java.net.URI
import java.util.concurrent.TimeUnit

import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.{BasicAuthentication, StringContentProvider}
import org.eclipse.jetty.http.{HttpMethod, HttpStatus, HttpVersion}
import org.slf4j.LoggerFactory

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

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val urls = hosts.map { host => new URI("http://" + host) }
  private val client = new HttpClient
  setAuthenticationParameters()
  client.start()

  def post(data: String, contentType: String): Boolean = {
    logger.debug(s"Post '$data' with contentType '$contentType' to REST service")
    trySendRequest(_
      .method(HttpMethod.POST)
      .content(new StringContentProvider(data), contentType)
    )
  }

  def delete(field: String, value: String): Boolean = {
    logger.debug(s"Delete entity with '$field' = '$value' fro REST service")
    trySendRequest(_
      .method(HttpMethod.DELETE)
      .param(field, value)
    )
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

  private def trySendRequest(requestModification: Request => Request): Boolean = {
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
}
