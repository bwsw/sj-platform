package com.bwsw.sj.crud.rest.validator.config

import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

import scala.collection.mutable.ArrayBuffer

object ConfigSettingValidator {

  val configService = ConnectionRepository.getConfigService

  def validate(initialData: ConfigSetting) = {

    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'name' can not be empty"
        } else {
          if (configService.get(x) != null) {
            errors += s"Config element with name $x already exists"
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
