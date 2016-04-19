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

    val instance = instanceDAO.retrieve(parameters.name)
    instance match {
      case Some(_) => errors += s"Instance for name: ${parameters.name} is exist."
    }

    if (listHasDoubles(parameters.inputs)) {
      errors += s"Inputs is not unique."
    }

    if (parameters.inputs.exists(s => !s.endsWith("/full") && !s.endsWith("/split"))) {
      errors += s"Inputs has incorrect name."
    }

    if (listHasDoubles(parameters.outputs)) {
      errors += s"Outputs is not unique."
    }

    val partitions = getPartitionForStreams(parameters.inputs)
    val minPartitionCount = partitions.values.min

    if (parameters.parallelism > minPartitionCount) {
      errors += s"Parallelism (${parameters.parallelism}) > minimum of partition count ($minPartitionCount) in all input stream."
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

    (errors, partitions)
  }

  /**
    * Check doubles in list
    * @param list - list for checking
    * @return - true, if list contain doubles
    */
  def listHasDoubles(list: List[String]): Boolean = {
    list.map(x => (x, 1)).groupBy(_._1).map(x => x._2.reduce { (a, b) => (a._1, a._2 + b._2) }).exists(x => x._2 > 1)
  }

  //todo потом доделать
  /**
    * Get count of partition for streams
    * @return - count of partition for each stream
    */
  def getPartitionForStreams(streams: List[String]) = {
    Map("s1" -> 11, "s2" -> 12, "s3" -> 15)
  }

}
