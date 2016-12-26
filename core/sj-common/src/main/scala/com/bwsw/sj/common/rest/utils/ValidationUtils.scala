package com.bwsw.sj.common.rest.utils

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.common.utils.ServiceLiterals._

import scala.collection.mutable.ArrayBuffer

trait ValidationUtils extends MessageResourceUtils {
  private val providerDAO = ConnectionRepository.getProviderService

  def validateName(name: String) = {
    name.matches( """^([a-z][a-z0-9-]*)$""")
  }

  def validateConfigSettingName(name: String) = {
    name.matches( """^([a-z][a-z-\.]*)$""")
  }

  def validateProvider(provider: String, serviceType: String) = {
    val errors = new ArrayBuffer[String]()

    Option(provider) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Provider")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Provider")
        }
        else {
          val providerObj = providerDAO.get(x)
          if (providerObj.isEmpty) {
            errors += createMessage("entity.error.doesnot.exist", "Provider", x)
          } else if (providerObj.get.providerType != typeToProviderType(serviceType)) {
            errors += createMessage("entity.error.must.one.type.other.given", "Provider", typeToProviderType(serviceType), providerObj.get.providerType)
          }
        }
    }

    errors
  }

  def validateNamespace(namespace: String) = {
    namespace.matches( """^([a-z][a-z0-9_]*)$""")
  }

  def normalizeName(name: String) = {
    name.replace('\\', '/')
  }
}
