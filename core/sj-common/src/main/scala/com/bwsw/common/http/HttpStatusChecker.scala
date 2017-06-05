package com.bwsw.common.http

import org.apache.http.HttpStatus
import org.apache.http.client.methods.CloseableHttpResponse

object HttpStatusChecker {
  def isStatusOK(response: CloseableHttpResponse): Boolean = {
    getStatusCode(response) == HttpStatus.SC_OK
  }

  def isStatusOK(statusCode: Int): Boolean = {
    statusCode == HttpStatus.SC_OK
  }

  def isStatusCreated(statusCode: Int): Boolean = {
    statusCode == HttpStatus.SC_CREATED
  }

  def isStatusNotFound(response: CloseableHttpResponse): Boolean = {
    getStatusCode(response) == HttpStatus.SC_NOT_FOUND
  }

  def isStatusNotFound(statusCode: Int): Boolean = {
    statusCode == HttpStatus.SC_NOT_FOUND
  }

  def getStatusCode(response: CloseableHttpResponse): Int = {
    response.getStatusLine.getStatusCode
  }
}
