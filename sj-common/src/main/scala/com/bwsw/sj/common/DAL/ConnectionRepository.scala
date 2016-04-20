package com.bwsw.sj.common.DAL

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.ConfigLoader
import com.mongodb.casbah.MongoClient

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

  def getFileMetadataDAO = {
    fileMetadataDAO
  }

  def getInstanceDAO = {
    instanceDAO
  }

  def getFileStorage = {
    new MongoFileStorage(mongoConnection(databaseName))
  }
}

object ConnectionConstants {
  val conf = ConfigLoader.load()
  val host = System.getenv("MONGO_HOST")
  val port = System.getenv("MONGO_PORT").toInt

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instancies"
}