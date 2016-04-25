package com.bwsw.sj.common.DAL

import com.bwsw.common.DAL.GenericMongoDAO
import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.common.entities.{Provider, Service, Streams}
import com.bwsw.tstreams.coordination.Coordinator
import com.mongodb.casbah.MongoClient
import org.redisson.{Redisson, Config}

/**
  * Repository for connection to MongoDB and file storage (GridFS)
  */
object ConnectionRepository {
  import ConnectionConstants._

  private val serializer = new JsonSerializer()
  serializer.setIgnoreUnknown(true)

  private lazy val mongoConnection = MongoClient(host, port)

  private lazy val fileMetadataDAO = new FileMetadataDAO(mongoConnection(databaseName)(fileMetadataCollection), serializer)

  private lazy val instanceDAO = new InstanceMetadataDAO(mongoConnection(databaseName)(instanceCollection), serializer)

  private lazy val streamsDAO = new GenericMongoDAO[Streams](mongoConnection(databaseName)(streamsCollection), serializer)

  private lazy val serviceDAO = new GenericMongoDAO[Service](mongoConnection(databaseName)(serviceCollection), serializer)

  private lazy val providerDAO = new GenericMongoDAO[Provider](mongoConnection(databaseName)(providerCollection), serializer)

  def getFileMetadataDAO = {
    fileMetadataDAO
  }

  def getInstanceDAO = {
    instanceDAO
  }

  def getFileStorage = {
    new MongoFileStorage(mongoConnection(databaseName))
  }

  def getStreamsDAO = {
    streamsDAO
  }

  def getServiceDAO = {
    serviceDAO
  }

  def getProviderDAO = {
    providerDAO
  }

}

object ConnectionConstants {
  val conf = ConfigLoader.load()
  val host = System.getenv("MONGO_HOST")
  val port = System.getenv("MONGO_PORT").toInt

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instances"
  lazy val streamsCollection = "streams"
  lazy val serviceCollection = "services"
  lazy val providerCollection = "providers"

}