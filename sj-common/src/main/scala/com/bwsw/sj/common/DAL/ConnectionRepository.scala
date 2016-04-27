package com.bwsw.sj.common.DAL

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.common.entities.{Provider, Service, SjStream}
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

  private lazy val mongoClient = new MongoClient("192.168.1.180", 27017) //new MongoClient(host, port)

  private lazy val morphia = new Morphia()

  private lazy val datastore = morphia.createDatastore(mongoClient, databaseName)

  private lazy val mongoConnection = com.mongodb.casbah.MongoClient(host, port)

  private lazy val fileMetadataDAO = new FileMetadataDAO(mongoConnection(databaseName)(fileMetadataCollection), serializer)

  private lazy val instanceService = new InstanceMetadataDAO(mongoConnection(databaseName)(instanceCollection), serializer)

  private lazy val streamService = new GenericMongoService[SjStream]()

  private lazy val serviceManager = new GenericMongoService[Service]()

  private lazy val providerService = new GenericMongoService[Provider]()

  def getFileMetadataDAO = {
    fileMetadataDAO
  }

  def getInstanceDAO = {
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

  private[DAL] def getGenericDAO[T: ClassTag] = {
    import scala.reflect.classTag
    val clazz: Class[T] = classTag[T].runtimeClass.asInstanceOf[Class[T]]
    new BasicDAO[T, String](clazz, datastore)
  }

}

object ConnectionConstants {
  val conf = ConfigLoader.load()
  val host = System.getenv("MONGO_HOST")
  val port = System.getenv("MONGO_PORT").toInt

  val databaseName = "stream_juggler"
  lazy val fileMetadataCollection = "fs.files"
  lazy val instanceCollection = "instances"
  lazy val streamCollection = "streams"
  lazy val serviceCollection = "services"
  lazy val providerCollection = "providers"

}