package com.bwsw.sj.common.rest.utils

import org.apache.curator.utils.PathUtils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Provides helping methods for validation some fields of entities
  */
object ValidationUtils {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def validateName(name: String): Boolean = {
    logger.debug(s"Validate a name: '$name'.")
    name.matches( """^([a-z][a-z0-9-]*)$""")
  }

  def validateConfigSettingName(name: String): Boolean = {
    logger.debug(s"Validate a configuration name: '$name'.")
    name.matches( """^([a-z][a-z0-9-\.]*)$""")
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