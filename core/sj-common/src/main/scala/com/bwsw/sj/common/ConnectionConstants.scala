package com.bwsw.sj.common

import com.bwsw.sj.common.utils.CommonAppConfigNames
import com.typesafe.config.ConfigFactory

import scala.util.Try

object ConnectionConstants {
  private val config = ConfigFactory.load()
  val mongoHosts: String = config.getString(CommonAppConfigNames.mongoHosts)
  val databaseName: String = config.getString(CommonAppConfigNames.mongoDbName)
  val mongoUser: Option[String] = Try(config.getString(CommonAppConfigNames.mongoUser)).toOption
  val mongoPassword: Option[String] = Try(config.getString(CommonAppConfigNames.mongoPassword)).toOption
}