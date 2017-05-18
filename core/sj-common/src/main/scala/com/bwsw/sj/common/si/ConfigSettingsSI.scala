package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.utils.MessageResourceUtils._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ConfigSettingsSI extends ServiceInterface[ConfigurationSetting, ConfigurationSettingDomain] {
  override protected val entityRepository: GenericMongoRepository[ConfigurationSettingDomain] = ConnectionRepository.getConfigRepository

  def create(entity: ConfigurationSetting): Either[ArrayBuffer[String], Boolean] = {
    val errors = new ArrayBuffer[String]
    errors ++= entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity.to())

      Right(true)
    } else {
      Left(errors)
    }
  }

  def getAll(): mutable.Buffer[ConfigurationSetting] = {
    entityRepository.getAll.map(x => ConfigurationSetting.from(x))
  }

  def get(name: String): Option[ConfigurationSetting] = {
    entityRepository.get(name).map(ConfigurationSetting.from)
  }

  def delete(name: String): Either[String, Boolean] = {
    var response: Either[String, Boolean] = Left(createMessage("rest.providers.provider.cannot.delete", name))
    val configSetting = entityRepository.get(name)
    configSetting match {
      case Some(_) =>
        entityRepository.delete(name)
        response = Right(true)
      case None =>
        response = Right(false)
    }

    response
  }

  def getDomain(parameters: Map[String, Any]): mutable.Buffer[ConfigurationSetting] = {
    entityRepository.getByParameters(parameters).map(ConfigurationSetting.from)
  }
}
