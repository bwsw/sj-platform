package com.bwsw.sj.crud.rest.validator.config

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.config.ConfigurationSettingData
import com.bwsw.sj.crud.rest.utils.ValidationUtils
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object ConfigSettingValidator extends ValidationUtils{

  private val logger = LoggerFactory.getLogger(getClass.getName)
  val configService = ConnectionRepository.getConfigService

  def validate(initialData: ConfigurationSettingData) = {
    logger.debug(s"Config setting ${initialData.name}. Start config settings validation.")

    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'Name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Name' can not be empty"
        } else {
          if (configService.get(x).isDefined) {
            errors += s"Config setting with name '$x' already exists"
          }

          if (!validateName(x)) {
            errors += s"Config setting has incorrect name: $x. " +
              s"Name of config setting must be contain digits, lowercase letters or hyphens. First symbol must be a letter."
          }
        }
    }

    // 'value' field
    Option(initialData.value) match {
      case None =>
        errors += s"'Value' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'Value' can not be empty"
    }

    errors
  }
}
