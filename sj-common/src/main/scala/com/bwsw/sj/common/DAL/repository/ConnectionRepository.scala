package com.bwsw.sj.common.DAL.repository

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.mongodb.MongoClient
import org.mongodb.morphia.Morphia
import org.mongodb.morphia.dao.BasicDAO

import scala.reflect.ClassTag

/**
 * Repository for connection to MongoDB and file storage (GridFS)
 */
object ConnectionRepository {

  import ConnectionConstants._

  private val serializer = new JsonSerializer()
  serializer.setIgnoreUnknown(true)

  private lazy val mongoClient = new MongoClient(mongoHost, mongoPort)

  private lazy val morphia = new Morphia()
  morphia.map(classOf[SjStream],classOf[Service], classOf[Provider])

  private lazy val datastore = morphia.createDatastore(mongoClient, databaseName)

  private lazy val mongoConnection = com.mongodb.casbah.MongoClient(mongoHost, mongoPort)

  private lazy val fileMetadataService = new GenericMongoService[FileMetadata]()

  private lazy val instanceService = new GenericMongoService[RegularInstance]()

  private lazy val streamService = new GenericMongoService[SjStream]()

  private lazy val serviceManager = new GenericMongoService[Service]()

  private lazy val providerService = new GenericMongoService[Provider]()

  def getFileMetadataService = {
    fileMetadataService
  }

  def getInstanceService = {
    instanceService
  }

  def getFileStorage = {
    new MongoFileStorage(mongoConnection(databaseName))
  }

  def getStreamService = {
    streamService
  }

  def getServiceManager = {
    serviceManager
  }

  def getProviderService = {
    providerService
  }

  def close() = {
    mongoConnection.close()
    mongoClient.close()
  }

  private[DAL] def getGenericDAO[T: ClassTag] = {
    import scala.reflect.classTag
    val clazz: Class[T] = classTag[T].runtimeClass.asInstanceOf[Class[T]]
    new BasicDAO[T, String](clazz, datastore)
  }
}

