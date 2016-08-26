package com.bwsw.sj.crud.rest.validator.config

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.entities.config.ConfigurationSettingData
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object ConfigSettingValidator {

  private val logger = LoggerFactory.getLogger(getClass.getName)
  val configService = ConnectionRepository.getConfigService

  def validate(initialData: ConfigurationSettingData) = {
    logger.debug(s"Config setting ${initialData.name}. Start config settings validation.")

    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'name' can not be empty"
        } else {
          if (configService.get(x).isDefined) {
            errors += s"Config setting with name $x already exists"
          }
          if (x.contains(" ")) {
            errors += s"Name $x of config setting contains spaces"
          }
        }
    }

    // 'value' field
    Option(initialData.value) match {
      case None =>
        errors += s"'value' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'value' can not be empty"
    }

    errors
  }
}
