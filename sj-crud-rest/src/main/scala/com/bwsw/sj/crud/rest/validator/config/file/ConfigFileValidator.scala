package com.bwsw.sj.crud.rest.validator.config.file

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.entities.ConfigElementData

import scala.collection.mutable.ArrayBuffer

object ConfigFileValidator {

  val configFileService = ConnectionRepository.getConfigFileService

  def validate(initialData: ConfigElementData) = {

    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'name' can not be empty"
        } else {
          if (configFileService.get(x) != null) {
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
