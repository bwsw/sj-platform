package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.KafkaSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{Service, Stream}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ArrayBuffer

class KafkaSjStreamData() extends SjStreamData() {
  streamType = Stream.kafkaStreamType
  var partitions: Int = 0
  @JsonProperty("replication-factor") var replicationFactor: Int = 0

  override def validate() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    Option(this.service) match {
      case None =>
        errors += s"'Service' is required"
      case Some(x) =>
        val serviceObj = serviceDAO.get(this.service)
        serviceObj match {
          case None =>
            errors += s"Service '${this.service}' does not exist"
          case Some(service) =>
            if (service.serviceType != Service.kafkaType) {
              errors += s"Service for ${Stream.kafkaStreamType} stream " +
                s"must be of '${Service.kafkaType}' type ('${service.serviceType}' is given instead)"
            }
        }
    }

    //partitions
    if (this.partitions <= 0)
      errors += s"'Partitions' must be a positive integer"

    //replicationFactor
    if (this.replicationFactor <= 0) {
      errors += s"'Replication-factor' must be a positive integer"
    }

    errors
  }

  override def asModelStream() = {
    val modelStream = new KafkaSjStream()
    super.fillModelStream(modelStream)
    modelStream.partitions = this.partitions
    modelStream.replicationFactor = this.replicationFactor

    modelStream
  }
}
