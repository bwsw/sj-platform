package com.bwsw.sj.common.DAL

import com.bwsw.common.DAL.GenericMongoDAO
import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.common.entities.InstanceMetadata
import com.mongodb.casbah.MongoClient

/**
  * Repository for connection to MongoDB and file storage (GridFS)
  */
object ConnectionRepository {
  import ConfigConstants._

  private val serializer = new JsonSerializer()
  serializer.setIgnoreUnknown(true)

  private lazy val mongoConnection = MongoClient(host, port)

  private lazy val fileMetadataDAO = new FileMetadataDAO(mongoConnection(databaseName)(fileMetadataCollection), serializer)

  private lazy val regularInstanceDAO = new GenericMongoDAO[InstanceMetadata](mongoConnection(databaseName)(regularInstanceCollection), serializer)

  private lazy val windowedInstanceDAO = new GenericMongoDAO[InstanceMetadata](mongoConnection(databaseName)(windowedInstanceCollection), serializer)

  private lazy val instanceDAOMap = Map(regularInstanceCollection -> regularInstanceDAO,
    windowedInstanceCollection -> windowedInstanceDAO)

  def getFileMetadataDAO = {
    fileMetadataDAO
  }

  def getInstanceDAO(collectionName: String) = {
    instanceDAOMap.get(collectionName).get
  }

  def getFileStorage = {
    new MongoFileStorage(mongoConnection(databaseName))
  }
}

object ConfigConstants {
  val conf = ConfigLoader.load()
  val host = "localhost"//sys.env("MONGO_HOST")
  val port = sys.env("MONGO_PORT").toInt

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val regularInstanceCollection = conf.getString("modules.regular-streaming.collection-name")
  lazy val windowedInstanceCollection = conf.getString("modules.time-windowed-streaming.collection-name")
}