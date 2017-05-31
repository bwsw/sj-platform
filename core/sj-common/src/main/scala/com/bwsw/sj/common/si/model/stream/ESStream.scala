package com.bwsw.sj.common.si.model.stream

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.dal.model.stream.ESStreamDomain
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class ESStream(name: String,
               service: String,
               tags: Array[String],
               force: Boolean,
               streamType: String,
               description: String)
              (implicit injector: Injector)
  extends SjStream(streamType, name, service, tags, force, description) {

  import messageResourceUtils.createMessage

  override def to(): ESStreamDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new ESStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[ESServiceDomain],
      description,
      force,
      tags)
  }

  override def create(): Unit =
    if (force) clearEsStream()

  override def delete(): Unit = clearEsStream()

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralFields()

    Option(service) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        val serviceDAO = connectionRepository.getServiceRepository
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

    errors
  }

  private def clearEsStream(): Unit = {
    val serviceDAO = connectionRepository.getServiceRepository
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
