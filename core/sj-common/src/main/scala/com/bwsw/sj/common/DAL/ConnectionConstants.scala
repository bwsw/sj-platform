package com.bwsw.sj.common.DAL

import com.mongodb.{MongoCredential, ServerAddress}

object ConnectionConstants {
  require(System.getenv("MONGO_HOSTS") != null,
    "No environment variables: MONGO_HOSTS")

  val mongoHosts = System.getenv("MONGO_HOSTS").split(",").toList.map(host => new ServerAddress(host.trim.split(":")(0), host.trim.split(":")(1).toInt))
  val mongoUser:Option[String] = Option(System.getenv("MONGO_USER"))
  val mongoPassword:Option[String] = Option(System.getenv("MONGO_PASSWORD"))
  var auth: Boolean = false

  if (mongoUser.isDefined && mongoPassword.isDefined) auth = true

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instances"
  lazy val streamCollection = "streams"
  lazy val serviceCollection = "services"
  lazy val providerCollection = "providers"
  lazy val configCollection = "config.file"

  lazy val mongoCredential = List(MongoCredential.createCredential(mongoUser.getOrElse(""), databaseName, mongoPassword.getOrElse("").toCharArray))
}
