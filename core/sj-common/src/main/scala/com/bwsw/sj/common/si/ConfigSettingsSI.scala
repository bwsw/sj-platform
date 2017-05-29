package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.si.result._

import scala.collection.mutable

/**
  * Provides methods to access [[ConfigurationSetting]]s in [[GenericMongoRepository]]
  */
class ConfigSettingsSI extends ServiceInterface[ConfigurationSetting, ConfigurationSettingDomain] {
  override protected val entityRepository: GenericMongoRepository[ConfigurationSettingDomain] = ConnectionRepository.getConfigRepository

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
    entityRepository.getAll.map(x => ConfigurationSetting.from(x))
  }

  def get(name: String): Option[ConfigurationSetting] = {
    entityRepository.get(name).map(ConfigurationSetting.from)
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

  def getBy(domain: String): mutable.Buffer[ConfigurationSetting] = {
    entityRepository.getByParameters(Map("domain" -> domain)).map(ConfigurationSetting.from)
  }
}
