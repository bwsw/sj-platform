package com.bwsw.sj.common.DAL

import com.mongodb.ServerAddress

object ConnectionConstants {
  require(System.getenv("MONGO_HOSTS") != null,
    "No environment variables: MONGO_HOSTS")

  val mongoHosts = System.getenv("MONGO_HOSTS").split(",").toList.map(x => new ServerAddress(x.trim))

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instances"
  lazy val streamCollection = "streams"
  lazy val serviceCollection = "services"
  lazy val providerCollection = "providers"
  lazy val configCollection = "config.file"
}
