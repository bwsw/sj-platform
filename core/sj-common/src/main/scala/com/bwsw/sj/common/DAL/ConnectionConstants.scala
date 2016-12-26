package com.bwsw.sj.common.DAL

import com.mongodb.ServerAddress

object ConnectionConstants {
  require(System.getenv("MONGO_HOSTS") != null,
    "No environment variables: MONGO_HOSTS")

  val mongoHosts = System.getenv("MONGO_HOSTS").split(",").toList.map(x => new ServerAddress(x.trim.split(":")(0), x.trim.split(":")(1).toInt))

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instances"
  lazy val streamCollection = "streams"
  lazy val serviceCollection = "services"
  lazy val providerCollection = "providers"
  lazy val configCollection = "config.file"
}
