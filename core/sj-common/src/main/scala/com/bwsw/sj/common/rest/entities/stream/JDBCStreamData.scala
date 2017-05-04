package com.bwsw.sj.common.rest.entities.stream

import com.bwsw.sj.common.DAL.model.stream.JDBCSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}

import scala.collection.mutable.ArrayBuffer

class JDBCStreamData() extends StreamData() {
  streamType = StreamLiterals.jdbcOutputType
  var primary: String = null

  override def validate(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceManager
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

   override def asModelStream(): JDBCSjStream = {
    val modelStream = new JDBCSjStream()
    super.fillModelStream(modelStream)
     modelStream.primary = this.primary

    modelStream
  }
}
