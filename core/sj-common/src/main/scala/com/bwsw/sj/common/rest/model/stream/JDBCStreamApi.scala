package com.bwsw.sj.common.rest.model.stream

import com.bwsw.sj.common.dal.model.service.JDBCServiceDomain
import com.bwsw.sj.common.dal.model.stream.JDBCStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.stream.JDBCStream
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

class JDBCStreamApi(val primary: String,
                    override val name: String,
                    override val service: String,
                    override val tags: Array[String] = Array(),
                    override val force: Boolean = false,
                    override val description: String = RestLiterals.defaultDescription)
  extends StreamApi(StreamLiterals.jdbcOutputType, name, service, tags, force, description) {

  override def to: JDBCStream =
    new JDBCStream(name, service, primary, tags, force, streamType, description)

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
              if (someService.serviceType != ServiceLiterals.jdbcType) {
                errors += createMessage("entity.error.must.one.type.other.given",
                  s"Service for '${StreamLiterals.jdbcOutputType}' stream",
                  ServiceLiterals.jdbcType,
                  someService.serviceType)
              }
          }
        }
    }

    errors
  }

  override def asModelStream(): JDBCStreamDomain = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val modelStream = new JDBCStreamDomain(
      this.name,
      serviceDAO.get(this.service).get.asInstanceOf[JDBCServiceDomain],
      this.primary,
      this.description,
      this.force,
      this.tags
    )

    modelStream
  }
}
