package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.DAL.ConnectionRepository

import scala.collection.mutable.ArrayBuffer

/**
  * Trait of validator for modules
  * Created: 4/12/2016
  *
  * @author Kseniya Tomskikh
  */
class ModuleValidator {
  private val startFromOptions = Set("oldest", "newest")
  private val stateManagementOptions = Set("oldest", "newest")
  val instanceDAO = ConnectionRepository.getInstanceDAO

  def validate(options: Map[String, Any]) = {
    val errors = new ArrayBuffer[String]()

    val name = options.get("name").get.asInstanceOf[String]
    if (instanceDAO.retrieve(name) != null) {
      errors += s"Instance for name: $name is exist"
    }

    val inputs = options.get("inputs").get.asInstanceOf[List[String]]
    val outputs = options.get("outputs").get.asInstanceOf[List[String]]

    val checkpointMode = options.get("checkpoint-mode").get.asInstanceOf[String]

    if (!options.get("checkpoint-interval").get.isInstanceOf[Int]) {
      errors += s"Checkpoint-interval attribute is not Integer value"
    }

    val stateManagement = options.get("state-management").get.asInstanceOf[String]

    if (!options.get("checkpoint-full-interval").get.isInstanceOf[Int]) {
      errors += s"Checkpoint-full-interval attribute is not Integer value"
    }

    if (!options.get("parallelism").get.isInstanceOf[Int]) {
      errors += s"Parallelism attribute is not Integer value"
    }

    val startFrom = options.get("start-from").get
    if (!checkStartFrom(startFrom)) {
      errors += s"Start-from attribute is invalid (must be 'oldest', 'newest' or timestamp (Long)"
    }

    errors.toList
  }

  def checkStartFrom(option: Any) = {
    if (!startFromOptions.contains(option.asInstanceOf[String])) {
      option.isInstanceOf[Long]
    } else true
  }

  def checkStateManagement(state: String) = {
    stateManagementOptions.contains(state)
  }
}
