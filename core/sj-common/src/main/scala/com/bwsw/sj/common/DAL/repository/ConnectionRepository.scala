package com.bwsw.sj.common.DAL.repository

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.model.service.Service
import com.bwsw.sj.common.DAL.model.stream.SjStream
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.mongodb.MongoClient
import org.mongodb.morphia.Morphia
import org.mongodb.morphia.dao.BasicDAO
import org.mongodb.morphia.mapping.DefaultCreator
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import collection.JavaConverters._

/**
 * Repository for connection to MongoDB and file storage (GridFS)
 */

object ConnectionRepository {

  import ConnectionConstants._

  private val logger = LoggerFactory.getLogger(this.getClass)

  private lazy val Left(mongoClient) = createClient("mongodb-driver") // new MongoClient(mongoHosts.asJava, mongoCredential.asJava)

  private lazy val morphia = new Morphia()
  morphia.map(classOf[SjStream]).map(classOf[Service]).map(classOf[Provider]).map(classOf[ConfigurationSetting]).map(classOf[Instance])

  changeGettingClassLoaderForMongo()

  private lazy val datastore = morphia.createDatastore(mongoClient, databaseName)

  private lazy val Right(mongoConnection) = createClient("casbah") // com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts, credentials = mongoCredential)

  private lazy val fileStorage: MongoFileStorage = new MongoFileStorage(mongoConnection(databaseName))

  private lazy val fileMetadataService = new GenericMongoService[FileMetadata]()

  private lazy val instanceService = new GenericMongoService[Instance]()

  private lazy val streamService = new GenericMongoService[SjStream]()

  private lazy val serviceManager = new GenericMongoService[Service]()

  private lazy val providerService = new GenericMongoService[Provider]()

  private lazy val configService = new GenericMongoService[ConfigurationSetting]()

  def getFileMetadataService = {
    fileMetadataService
  }

  def getConfigService = {
    configService
  }

  def getInstanceService = {
    instanceService
  }

  def getFileStorage = {
    fileStorage
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
    logger.debug("Close a repository of connection.")
    mongoConnection.close()
    mongoClient.close()
  }

  def createClient(clientType: String): Either[MongoClient, com.mongodb.casbah.MongoClient] = {
    logger.debug("Create a new mongo client.")
    if (authEnable) {
      clientType match {
        case "mongodb-driver" => Left(new MongoClient(mongoHosts.asJava, mongoCredential.asJava))
        case "casbah" => Right(com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts, credentials = mongoCredential))
      }
    }
    else {
      clientType match {
        case "mongodb-driver" => Left(new MongoClient(mongoHosts.asJava))
        case "casbah" => Right(com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts))
      }
    }
  }

  private[DAL] def getGenericDAO[T: ClassTag] = {
    import scala.reflect.classTag

    logger.debug(s"Create a basic DAO for a mongo collection of type: '${classTag[T].toString()}'.")
    val clazz: Class[T] = classTag[T].runtimeClass.asInstanceOf[Class[T]]
    new BasicDAO[T, String](clazz, datastore)
  }

  /**
   * It's necessary because of when a MesosSchedulerDriver (in mesos framework) is being created something is going wrong
   * (probably it should be but it's not our case) and after it the all instances have a null value of class loader.
   * May be it is a temporary measure (if we will find a different solution)
   */
  private def changeGettingClassLoaderForMongo() = {
    morphia.getMapper.getOptions.setObjectFactory(new DefaultCreator() {
      override def getClassLoaderForClass = {
        classOf[JsonSerializer].getClassLoader
      }
    })
  }
}