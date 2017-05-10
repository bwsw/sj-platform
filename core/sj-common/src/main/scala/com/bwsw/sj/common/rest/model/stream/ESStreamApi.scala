package com.bwsw.sj.common.rest.model.stream

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.dal.model.stream.ESStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

class ESStreamApi() extends StreamApi() {
  streamType = StreamLiterals.esOutputType

  override def validate(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    Option(this.service) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Service")
        }
        else {
          val serviceObj = serviceDAO.get(x)
          serviceObj match {
            case None =>
              errors += createMessage("entity.error.doesnot.exist", "Service", x)
            case Some(someService) =>
              if (someService.serviceType != ServiceLiterals.elasticsearchType) {
                errors += createMessage("entity.error.must.one.type.other.given",
                  s"Service for '${StreamLiterals.esOutputType}' stream",
                  ServiceLiterals.elasticsearchType,
                  someService.serviceType)
              }
          }
        }
    }

    errors
  }

  override def asModelStream(): ESStreamDomain = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val modelStream = new ESStreamDomain(
      this.name,
      serviceDAO.get(this.service).get.asInstanceOf[ESServiceDomain],
      this.description,
      this.force,
      this.tags
    )

    modelStream
  }

  override def create(): Unit = {
    if (doesStreamHaveForcedCreation()) {
      clearEsStream()
    }
  }

  private def doesStreamHaveForcedCreation(): Boolean = {
    this.force
  }

  private def clearEsStream(): Unit = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[ESServiceDomain]
    val hosts = service.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)
    client.deleteDocuments(service.index, this.name)

    client.close()
  }
}
