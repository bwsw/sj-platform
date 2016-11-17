package com.bwsw.sj.common.DAL

object ConnectionConstants {
  require(System.getenv("MONGO_HOST") != null && System.getenv("MONGO_PORT") != null,
    "No environment variables: MONGO_HOST, MONGO_PORT")
  
  val mongoHost = System.getenv("MONGO_HOST")
  val mongoPort = System.getenv("MONGO_PORT").toInt

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instances"
  lazy val streamCollection = "streams"
  lazy val serviceCollection = "services"
  lazy val providerCollection = "providers"
  lazy val configCollection = "config.file"
}
