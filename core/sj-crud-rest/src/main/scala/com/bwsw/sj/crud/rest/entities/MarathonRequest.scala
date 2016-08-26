package com.bwsw.sj.crud.rest.entities

/**
  * Protocol for creating and running application on mesos
  *
  *
  * @author Kseniya Tomskikh
  */
case class MarathonRequest(id: String,
                           cmd: String,
                           instances: Int,
                           env: Map[String, String],
                           uris: List[String])