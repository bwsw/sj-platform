package com.bwsw.sj.common

import com.mongodb._

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

class MongoAuthChecker(address: String, databaseName: String) {
  private val mongoHosts: List[ServerAddress] = address.split(",").toList
    .map(host => new ServerAddress(host.trim.split(":")(0), host.trim.split(":")(1).toInt))

  def isAuthRequired(): Boolean = {
    val client = com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts)

    checkConnection(client)
  }

  def isCorrectCredentials(userName: Option[String], password: Option[String]): Boolean = {
    val mongoCredential = List(MongoCredential.createCredential(userName.getOrElse(""), databaseName, password.getOrElse("").toCharArray))
    val client = com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts, credentials = mongoCredential)

    !checkConnection(client)
  }

  def createClient(clientType: String, authEnable: Boolean, userName: Option[String] = None, password: Option[String] = None): (MongoClient, com.mongodb.casbah.MongoClient) = {
    if (authEnable) {
      val mongoCredential = List(MongoCredential.createCredential(userName.getOrElse(""), databaseName, password.getOrElse("").toCharArray))

      (new MongoClient(mongoHosts.asJava, mongoCredential.asJava),
        com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts, credentials = mongoCredential)
      )
    }
    else {
      (new MongoClient(mongoHosts.asJava),
        com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts))
    }
  }

  private def checkConnection(client: com.mongodb.casbah.MongoClient): Boolean = {
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
