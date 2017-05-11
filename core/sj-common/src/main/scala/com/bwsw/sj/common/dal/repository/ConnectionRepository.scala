package com.bwsw.sj.common.dal.repository

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.dal.ConnectionConstants
import com.bwsw.sj.common.dal.model._
import com.bwsw.sj.common.dal.model.module.{FileMetadata, InstanceDomain}
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.morphia.CustomMorphiaObjectFactory
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

  private lazy val fileMetadataRepository = new GenericMongoRepository[FileMetadata]()

  private lazy val instanceRepository = new GenericMongoRepository[InstanceDomain]()

  private lazy val streamRepository = new GenericMongoRepository[StreamDomain]()

  private lazy val serviceRepository = new GenericMongoRepository[ServiceDomain]()

  private lazy val providerRepository = new GenericMongoRepository[ProviderDomain]()

  private lazy val configRepository = new GenericMongoRepository[ConfigurationSettingDomain]()

  def getFileMetadataRepository: GenericMongoRepository[FileMetadata] = {
    fileMetadataRepository
  }

  def getConfigRepository: GenericMongoRepository[ConfigurationSettingDomain] = {
    configRepository
  }

  def getInstanceRepository: GenericMongoRepository[InstanceDomain] = {
    instanceRepository
  }

  def getFileStorage: MongoFileStorage = {
    fileStorage
  }

  def getStreamRepository: GenericMongoRepository[StreamDomain] = {
    streamRepository
  }

  def getServiceRepository: GenericMongoRepository[ServiceDomain] = {
    serviceRepository
  }

  def getProviderRepository: GenericMongoRepository[ProviderDomain] = {
    providerRepository
  }

  def close(): Unit = {
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

  private def setMapperOptions(morphia: Morphia): Unit = {
    val mapper = morphia.getMapper
    mapper.getOptions.setObjectFactory(new CustomMorphiaObjectFactory())
    mapper.getOptions.setStoreEmpties(true)
  }

  private[dal] def getGenericDAO[T: ClassTag]: BasicDAO[T, String] = {
    import scala.reflect.classTag

    logger.debug(s"Create a basic DAO for a mongo collection of type: '${classTag[T].toString()}'.")
    val clazz: Class[T] = classTag[T].runtimeClass.asInstanceOf[Class[T]]
    new BasicDAO[T, String](clazz, datastore)
  }
}