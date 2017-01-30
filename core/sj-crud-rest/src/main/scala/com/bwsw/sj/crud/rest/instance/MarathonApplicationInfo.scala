package com.bwsw.sj.crud.rest.instance

case class MarathonApplicationById(apps: Array[MarathonApplicationInfo])
case class MarathonApplicationInfo(id: String, env: Map[String, String])