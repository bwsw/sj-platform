package com.bwsw.sj.common.dal

import com.mongodb._

import scala.util.{Failure, Success, Try}

object ConnectionConstants {
  require(Option(System.getenv("MONGO_HOSTS")).isDefined,
    "No environment variables: MONGO_HOSTS")

  val mongoHosts = System.getenv("MONGO_HOSTS").split(",").toList.map(host => new ServerAddress(host.trim.split(":")(0), host.trim.split(":")(1).toInt))

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instances"
  lazy val streamCollection = "streams"
  lazy val serviceCollection = "services"
  lazy val providerCollection = "providers"
  lazy val configCollection = "config.file"
  lazy val mongoUser:Option[String] = Option(System.getenv("MONGO_USER"))
  lazy val mongoPassword:Option[String] = Option(System.getenv("MONGO_PASSWORD"))

  var authEnable: Boolean = isAuthRequired

  var mongoEnvironment: Map[String, String] = Map[String, String]("MONGO_HOSTS" -> System.getenv("MONGO_HOSTS"))
  if (authEnable) {
    if ((mongoUser.nonEmpty && mongoPassword.nonEmpty) && isCorrectCredentials)
    mongoEnvironment = mongoEnvironment ++ Map[String, String](
      "MONGO_USER" -> mongoUser.get,
      "MONGO_PASSWORD" -> mongoPassword.get
    )
  }

  lazy val mongoCredential: List[MongoCredential] = List(MongoCredential.createCredential(mongoUser.getOrElse(""), databaseName, mongoPassword.getOrElse("").toCharArray))

  def isAuthRequired: Boolean = {
    val client = com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts)
    checkConnection(client)
  }

  def isCorrectCredentials: Boolean = {
    val client = com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts, credentials = mongoCredential)
    !checkConnection(client)
  }

  def checkConnection(client:com.mongodb.casbah.MongoClient): Boolean = {
    val result = Try {
      client(databaseName).collectionNames()
    }
    client.close()
    result match {
      case Success(_) => false
      case Failure(_: com.mongodb.MongoCommandException) => true
      case Failure(_: MongoTimeoutException) => throw new MongoClientException(s"Something went wrong: timeout exception caught. " +
        s"Check connection setting: hosts and credentials.")
      case Failure(e: MongoException) => throw new Exception(s"Unexpected exception: ${e.getMessage}, ${e.getClass}")
      case Failure(e) => throw e
    }
  }
}