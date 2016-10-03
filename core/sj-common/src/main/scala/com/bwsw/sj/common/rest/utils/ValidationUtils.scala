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
    val providerErrors = new ArrayBuffer[String]()
    serviceType match {
      case _ if types.contains(serviceType) =>
        Option(provider) match {
          case None =>
            providerErrors += s"'Provider' is required"
          case Some(p) =>
            val providerObj = providerDAO.get(p)
            if (providerObj.isEmpty) {
              providerErrors += s"Provider '$p' does not exist"
            } else if (providerObj.get.providerType != typeToProviderType(serviceType)) {
              providerErrors += s"Provider for '$serviceType' service must be of type '${typeToProviderType(serviceType)}' " +
                s"('${providerObj.get.providerType}' is given instead)"
            }
        }
    }

    providerErrors
  }

  def validateStringFieldRequired(fieldData: String, fieldJsonName: String) = {
    val errors = new ArrayBuffer[String]()

    Option(fieldData) match {
      case None =>
        errors += s"'$fieldJsonName' is required"
      case Some(x) =>
    }

    errors
  }

  def validateNamespace(namespace: String) = {
    val errors = new ArrayBuffer[String]()

    if (!validateServiceNamespace(namespace)) {
      errors += s"Service has incorrect parameter: $namespace. " +
        s"Name must contain digits, lowercase letters or underscore. First symbol must be a letter"
    }

    errors
  }

  private def validateServiceNamespace(namespace: String) = {
    namespace.matches( """^([a-z][a-z0-9_]*)$""")
  }
}
