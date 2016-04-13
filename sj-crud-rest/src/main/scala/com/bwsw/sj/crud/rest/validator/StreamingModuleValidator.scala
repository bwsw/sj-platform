package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.DAL.ConnectionRepository
import com.bwsw.sj.common.entities.InstanceMetadata

import scala.collection.mutable.ArrayBuffer

/**
  * Trait of validator for modules
  * Created: 4/12/2016
  *
  * @author Kseniya Tomskikh
  */
abstract class StreamingModuleValidator {
  private val startFromOptions = Set("oldest", "newest")
  private val stateManagementOptions = Set("oldest", "newest")
  val instanceDAO = ConnectionRepository.getInstanceDAO

  def validate(options: InstanceMetadata) = {
    val errors = new ArrayBuffer[String]()

    if (instanceDAO.retrieve(options.name) != null) {
      errors += s"Instance for name: ${options.name} is exist."
    }

    if (!stateManagementOptions.contains(options.stateManagement)) {
      errors += s"Unknown value of state-management attribute: ${options.stateManagement}. " +
        s"State-management must be 'none' or 'ram' or 'rocks'."
    }

    val startFrom = options.startFrom.asInstanceOf[String]
    if (!startFromOptions.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
      }
    }

    errors.toList
  }
}
