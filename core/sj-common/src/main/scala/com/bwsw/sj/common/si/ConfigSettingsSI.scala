package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.config.{ConfigurationSetting, CreateConfigurationSetting}
import com.bwsw.sj.common.si.result._
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable

/**
  * Provides methods to access [[ConfigurationSetting]]s in [[GenericMongoRepository]]
  */
class ConfigSettingsSI(implicit injector: Injector) extends ServiceInterface[ConfigurationSetting, ConfigurationSettingDomain] {
  private val connectionRepository = inject[ConnectionRepository]
  private val createConfigurationSetting = inject[CreateConfigurationSetting]
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
