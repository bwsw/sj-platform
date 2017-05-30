package com.bwsw.sj.common.rest.utils

import com.bwsw.sj.common.SjModule
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.ServiceLiterals._
import org.apache.curator.utils.PathUtils
import org.slf4j.LoggerFactory
import scaldi.Injectable.inject

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

/**
  * Provides helping methods for validation some fields of entities
  */
object ValidationUtils {

  import SjModule._

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val providerDAO = inject[ConnectionRepository].getProviderRepository

  def validateName(name: String): Boolean = {
    logger.debug(s"Validate a name: '$name'.")
    name.matches( """^([a-z][a-z0-9-]*)$""")
  }

  def validateConfigSettingName(name: String): Boolean = {
    logger.debug(s"Validate a configuration name: '$name'.")
    name.matches( """^([a-z][a-z0-9-\.]*)$""")
  }

  /**
    * Checks that provider exists and type of service corresponds to provider
    *
    * @param provider
    * @param serviceType
    * @return empty array if validation passed, collection of errors otherwise
    */
  def validateProvider(provider: String, serviceType: String): ArrayBuffer[String] = {
    logger.debug(s"Validate a provider: '$provider' of service: '$serviceType'.")
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

  def validateNamespace(namespace: String): Boolean = {
    logger.debug(s"Validate a namespace: '$namespace'.")
    namespace.matches( """^([a-z][a-z0-9_]*)$""")
  }

  def normalizeName(name: String): String = {
    logger.debug(s"Normalize a name: '$name'.")
    name.replace('\\', '/')
  }

  /**
    * Validates prefix in [[com.bwsw.sj.common.si.model.service.TStreamService TStreamService]]
    *
    * @param prefix
    * @return None if prefix is valid, Some(error) otherwise
    */
  def validatePrefix(prefix: String): Option[String] = {
    Try(PathUtils.validatePath(prefix)) match {
      case Success(_) => None
      case Failure(exception: Throwable) => Some(exception.getMessage)
    }
  }

  def validateToken(token: String): Boolean = {
    token.length <= 32
  }
}