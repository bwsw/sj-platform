package com.bwsw.sj.common.rest.utils

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals._

import scala.collection.mutable.ArrayBuffer

trait ValidationUtils {
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
        errors += "'Provider' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += "'Provider' is required"
        }
        else {
          val providerObj = providerDAO.get(x)
          if (providerObj.isEmpty) {
            errors += s"Provider '$x' does not exist"
          } else if (providerObj.get.providerType != typeToProviderType(serviceType)) {
            errors += s"Provider for '$serviceType' service must be of type '${typeToProviderType(serviceType)}' " +
              s"('${providerObj.get.providerType}' is given instead)"
          }
        }
    }

    errors
  }

  def validateNamespace(namespace: String) = {
    namespace.matches( """^([a-z][a-z0-9_]*)$""")
  }
}
