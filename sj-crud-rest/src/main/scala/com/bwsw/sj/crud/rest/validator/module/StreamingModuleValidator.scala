package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.ConnectionRepository
import com.bwsw.sj.common.entities.InstanceMetadata

import scala.collection.mutable.ArrayBuffer

/**
  * Trait of validator for modules
  * Created: 12/04/2016
  *
  * @author Kseniya Tomskikh
  */
abstract class StreamingModuleValidator {
  import com.bwsw.sj.common.module.ModuleConstants._

  /**
    * Validating input parameters for streaming module
    * @param parameters - input parameters for running module
    * @return - List of errors
    */
  def validate(parameters: InstanceMetadata) = {
    val instanceDAO = ConnectionRepository.getInstanceDAO
    val errors = new ArrayBuffer[String]()

    instanceDAO.retrieve(parameters.name) match {
      case None => errors += s"Instance for name: ${parameters.name} is exist."
    }

    if (listHasDoubles(parameters.inputs)) {
      errors += s"Inputs is not unique"
    }

    if (listHasDoubles(parameters.outputs)) {
      errors += s"Outputs is not unique"
    }

    if (!stateManagementModes.contains(parameters.stateManagement)) {
      errors += s"Unknown value of state-management attribute: ${parameters.stateManagement}. " +
        s"State-management must be 'none' or 'ram' or 'rocks'."
    }

    if (!checkpointModes.contains(parameters.checkpointMode)) {
      errors += s"Unknown value of checkpoint-mode attribute: ${parameters.checkpointMode}."
    }

    if (parameters.jvmOptions.isEmpty) {
      errors += "Jvm-options attribute is empty."
    }

    val startFrom = parameters.startFrom.asInstanceOf[String]
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"Start-from attribute is not 'oldest' or 'newest' or timestamp."
      }
    }

    errors
  }

  /**
    * Check doubles in list
    * @param list - list for checking
    * @return - true, if list contain doubles
    */
  def listHasDoubles(list: List[String]): Boolean = {
    list.map(x => (x, 1)).groupBy(_._1).map(x => x._2.reduce { (a, b) => (a._1, a._2 + b._2) }).exists(x => x._2 > 1)
  }
}
