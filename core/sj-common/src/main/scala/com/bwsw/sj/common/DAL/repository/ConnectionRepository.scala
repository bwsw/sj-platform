package com.bwsw.sj.common.DAL.repository

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{FileMetadata, Instance}
import com.bwsw.sj.common.DAL.model.provider.Provider
import com.bwsw.sj.common.DAL.model.service.Service
import com.bwsw.sj.common.DAL.model.stream.SjStream
import com.bwsw.sj.common.DAL.morphia.CustomMorphiaObjectFactory
import com.bwsw.sj.common.DAL.service.GenericMongoRepository
import com.mongodb.MongoClient
import org.mongodb.morphia.Morphia
import org.mongodb.morphia.dao.BasicDAO
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
  * Repository for connection to MongoDB and file storage (GridFS)
  */

object ConnectionRepository {

  import ConnectionConstants._

  private val logger = LoggerFactory.getLogger(this.getClass)

  private lazy val Left(mongoClient) = createClient("mongodb-driver") // new MongoClient(mongoHosts.asJava, mongoCredential.asJava)

  private lazy val morphia = new Morphia()
  setMapperOptions(morphia)

  private lazy val datastore = morphia.createDatastore(mongoClient, databaseName)

  private lazy val Right(mongoConnection) = createClient("casbah") // com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts, credentials = mongoCredential)

  private lazy val fileStorage: MongoFileStorage = new MongoFileStorage(mongoConnection(databaseName))

  private lazy val fileMetadataService = new GenericMongoRepository[FileMetadata]()

  private lazy val instanceService = new GenericMongoRepository[Instance]()

  private lazy val streamService = new GenericMongoRepository[SjStream]()

  private lazy val serviceManager = new GenericMongoRepository[Service]()

  private lazy val providerService = new GenericMongoRepository[Provider]()

  private lazy val configService = new GenericMongoRepository[ConfigurationSetting]()

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

  private def setMapperOptions(morphia: Morphia) = {
    val mapper = morphia.getMapper
    mapper.getOptions.setObjectFactory(new CustomMorphiaObjectFactory())
    mapper.getOptions.setStoreEmpties(true)
  }

  private[DAL] def getGenericDAO[T: ClassTag] = {
    import scala.reflect.classTag

    logger.debug(s"Create a basic DAO for a mongo collection of type: '${classTag[T].toString()}'.")
    val clazz: Class[T] = classTag[T].runtimeClass.asInstanceOf[Class[T]]
    new BasicDAO[T, String](clazz, datastore)
  }
}