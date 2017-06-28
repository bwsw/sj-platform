/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.dal.repository

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.{ConnectionConstants, MongoAuthChecker}
import com.bwsw.sj.common.dal.model._
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.morphia.CustomMorphiaObjectFactory
import org.mongodb.morphia.Morphia
import org.mongodb.morphia.dao.BasicDAO
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.reflect.ClassTag

/**
  * Repository for connection to MongoDB and file storage [[com.mongodb.casbah.gridfs.GridFS]]
  */
class ConnectionRepository(mongoAuthChecker: MongoAuthChecker)(implicit injector: Injector) {

  import ConnectionConstants._

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val authEnable: Boolean = mongoAuthChecker.isAuthRequired()

  private lazy val (mongoClient, mongoConnection) = mongoAuthChecker.createClient("mongodb-driver", authEnable, mongoUser, mongoPassword) // new MongoClient(mongoHosts.asJava, mongoCredential.asJava)
  // com.mongodb.casbah.MongoClient(replicaSetSeeds = mongoHosts, credentials = mongoCredential)

  private lazy val morphia = new Morphia()
  setMapperOptions(morphia)

  private lazy val datastore = morphia.createDatastore(mongoClient, databaseName)

  private lazy val fileStorage: MongoFileStorage = new MongoFileStorage(mongoConnection(databaseName))

  private lazy val fileMetadataRepository = new GenericMongoRepository[FileMetadataDomain]()

  private lazy val instanceRepository = new GenericMongoRepository[InstanceDomain]()

  private lazy val streamRepository = new GenericMongoRepository[StreamDomain]()

  private lazy val serviceRepository = new GenericMongoRepository[ServiceDomain]()

  private lazy val providerRepository = new GenericMongoRepository[ProviderDomain]()

  private lazy val configRepository = new GenericMongoRepository[ConfigurationSettingDomain]()

  var mongoEnvironment: Map[String, String] = Map[String, String]("MONGO_HOSTS" -> mongoHosts)
  if (authEnable) {
    if ((mongoUser.nonEmpty && mongoPassword.nonEmpty) && mongoAuthChecker.isCorrectCredentials(mongoUser, mongoPassword))
      mongoEnvironment = mongoEnvironment ++ Map[String, String](
        "MONGO_USER" -> mongoUser.get,
        "MONGO_PASSWORD" -> mongoPassword.get
      )
  }

  def getFileMetadataRepository: GenericMongoRepository[FileMetadataDomain] = {
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
