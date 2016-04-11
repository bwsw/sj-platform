package com.bwsw.sj.common.DAL

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
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

  def getFileMetadataDAO = {
    fileMetadataDAO
  }

  def getFileStorage = {
    new MongoFileStorage(mongoConnection(databaseName))
  }
}

object ConfigConstants {
  val host = "localhost"//sys.env("MONGO_HOST")
  val port = sys.env("MONGO_PORT").toInt

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
}