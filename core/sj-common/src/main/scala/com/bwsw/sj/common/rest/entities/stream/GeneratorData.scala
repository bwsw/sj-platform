package com.bwsw.sj.common.rest.entities.stream

import java.net.{URI, URISyntaxException}

import com.bwsw.sj.common.DAL.model.Generator
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.GeneratorLiterals._
import com.bwsw.sj.common.utils.{GeneratorLiterals, MessageResourceUtils, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ArrayBuffer

case class GeneratorData(@JsonProperty("generator-type") generatorType: String,
                         service: String = null,
                         @JsonProperty("instance-count") instanceCount: Int = Int.MinValue) extends ValidationUtils with MessageResourceUtils {

  def asModelGenerator() = {
    val serviceDAO = ConnectionRepository.getServiceManager
    this.generatorType match {
      case GeneratorLiterals.localType => new Generator(this.generatorType)
      case _ => new Generator(this.generatorType, serviceDAO.get(this.service).get, this.instanceCount)
    }
  }

  def validate() = {
    val errors = new ArrayBuffer[String]()

    Option(this.generatorType) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Generator-type")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Generator-type")
        }
        else {
          if (!types.contains(x)) {
            errors += createMessage("entity.error.unknown.type.must.one.of", x, "generator", types.mkString("[", ", ", "]"))
          } else {
            if (this.generatorType != GeneratorLiterals.localType) {
              //instacneCount
              if (this.instanceCount <= 0)
                errors += createMessage("entity.error.generator.attribute.required", "instance-count") + ". " +
                  createMessage("entity.error.generator.instance.count")

              //service
              Option(this.service) match {
                case None =>
                  errors += createMessage("entity.error.generator.attribute.required", "service")
                case Some(s) =>
                  if (s.isEmpty) {
                    errors += createMessage("entity.error.generator.attribute.required", "service")
                  }
                  else {
                    errors ++= validateService(s)
                  }
              }
            }
          }
        }
    }

    errors
  }

  private def validateService(service: String) = {
    val serviceDAO = ConnectionRepository.getServiceManager
    val errors = new ArrayBuffer[String]()
    try {
      val uri = new URI(normalizeName(service))
      var serviceName: String = ""
      if (uri.getScheme != null) {
        if (!uri.getScheme.equals("service-zk")) {
          errors += createMessage("entity.error.generator.service.invalid.uri")
        } else {
          serviceName = uri.getAuthority
        }
      } else {
        if (uri.toString.contains('/')) {
          errors += createMessage("entity.error.generator.service.invalid.uri")
        } else {
          serviceName = this.service
        }
      }

      val serviceObj = serviceDAO.get(serviceName)
      if (serviceObj.isEmpty) {
        errors += createMessage("entity.error.doesnot.exist", "Generator", "service")
      } else {
        if (serviceObj.get.serviceType != ServiceLiterals.zookeeperType) {
          errors += createMessage("entity.error.must.one.type.other.given", "Generator-service", ServiceLiterals.zookeeperType, serviceObj.get.serviceType)
        }
      }
    } catch {
      case _: URISyntaxException =>
        errors += createMessage("entity.error.generator.service.invalid.uri")
    }

    errors
  }
}