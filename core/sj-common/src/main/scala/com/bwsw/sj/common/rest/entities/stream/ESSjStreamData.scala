package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.common.ElasticsearchClient
import com.bwsw.sj.common.DAL.model.{ESService, ESSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

class ESSjStreamData() extends SjStreamData() {
  streamType = StreamLiterals.esOutputType

  override def validate() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    Option(this.service) match {
      case None =>
        errors += s"'Service' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Service' is required"
        }
        else {
          val serviceObj = serviceDAO.get(x)
          serviceObj match {
            case None =>
              errors += s"Service '$x' does not exist"
            case Some(someService) =>
              if (someService.serviceType != ServiceLiterals.elasticsearchType) {
                errors += s"Service for ${StreamLiterals.esOutputType} stream " +
                  s"must be of '${ServiceLiterals.elasticsearchType}' type ('${someService.serviceType}' is given instead)"
              }
          }
        }
    }

    errors
  }

  override def asModelStream() = {
    val modelStream = new ESSjStream()
    super.fillModelStream(modelStream)

    modelStream
  }

  override def create() = {
    if (doesStreamHaveForcedCreation()) {
      clearEsStream()
    }
  }

  private def doesStreamHaveForcedCreation() = {
    this.force
  }

  private def clearEsStream() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val service = serviceDAO.get(this.service).get.asInstanceOf[ESService]
    val hosts = service.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)
    val outputData = client.search(service.index, this.name)

    outputData.getHits.foreach { hit =>
      val id = hit.getId
      client.deleteDocumentByTypeAndId(service.index, this.name, id)
    }

    client.close()
  }
}
