package com.bwsw.sj.crud.rest.utils

import java.net.URI

object RestLiterals {
  final val masterNode = "/rest/instance/lock"

  val crudRestConfig = "crud-rest"
  val hostConfig = crudRestConfig + ".host"
  val portConfig = crudRestConfig + ".port"
  val corsAllowedOriginConfig = "cors.allowed-origin"

  def createUri(host: String, port: Int): String = {
    new URI(s"http://$host:$port").toString
  }
}
