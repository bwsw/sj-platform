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
package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.config.{ConfigurationSetting, ConfigurationSettingCreator}
import com.bwsw.sj.common.si.result._
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable

/**
  * Provides methods to access [[com.bwsw.sj.common.si.model.config.ConfigurationSetting]]s
  * in [[com.bwsw.sj.common.dal.repository.GenericMongoRepository]]
  */
class ConfigSettingsSI(implicit injector: Injector) extends ServiceInterface[ConfigurationSetting, ConfigurationSettingDomain] {
  private val connectionRepository = inject[ConnectionRepository]
  private val createConfigurationSetting = inject[ConfigurationSettingCreator]
  override protected val entityRepository: GenericMongoRepository[ConfigurationSettingDomain] = connectionRepository.getConfigRepository

  def create(entity: ConfigurationSetting): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity.to())

      Created
    } else {
      NotCreated(errors)
    }
  }

  def getAll(): mutable.Buffer[ConfigurationSetting] = {
    entityRepository.getAll.map(x => createConfigurationSetting.from(x))
  }

  def get(name: String): Option[ConfigurationSetting] = {
    entityRepository.get(name).map(createConfigurationSetting.from)
  }

  def delete(name: String): DeletionResult = {
    entityRepository.get(name) match {
      case Some(_) =>
        entityRepository.delete(name)

        Deleted
      case None =>
        EntityNotFound
    }
  }

  def getBy(domain: String): Seq[ConfigurationSetting] = {
    entityRepository.getByParameters(Map("domain" -> domain)).map(createConfigurationSetting.from)
  }
}
