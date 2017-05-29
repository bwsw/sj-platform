package com.bwsw.sj.common.si.model.module

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable.ArrayBuffer
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.util.{Failure, Success, Try}

class ModuleMetadata(filename: String,
                     val specification: Specification,
                     file: Option[File] = None,
                     name: Option[String] = None,
                     version: Option[String] = None,
                     length: Option[Long] = None,
                     description: Option[String] = None,
                     uploadDate: Option[String] = None)
  extends FileMetadata(
    filename,
    file,
    name,
    version,
    length,
    description,
    uploadDate) {

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]

    if (fileStorage.exists(filename))
      errors += createMessage("rest.modules.module.file.exists", filename)

    errors ++= specification.validate

    val implementations = List(("validator-class", specification.validatorClass, classOf[StreamingValidator]))

    val definitions = specification match {
      case (s: BatchSpecification) =>
        List(
          ("executor-class", s.executorClass),
          ("batch-collector-class", s.batchCollectorClass))
      case _ => List(("executor-class", specification.executorClass))
    }

    errors ++= validateClasses(implementations, definitions)

    errors
  }

  /**
    * Validates implementations of interfaces and existence classes in module.
    *
    * @param implementations list of (property name, class name, interface)
    * @param definitions     list of (property name, class name), if cannot validate implementation
    */
  def validateClasses(implementations: List[(String, String, Class[_])], definitions: List[(String, String)]) = {
    val errors = new ArrayBuffer[String]
    if (file.isDefined) {
      Try {
        new URLClassLoader(Array(file.get.toURI.toURL), ClassLoader.getSystemClassLoader)
      } match {
        case Success(classLoader) =>
          implementations.foreach {
            case (property, className, interface) =>
              Try(classLoader.loadClass(className)) match {
                case Success(implementation) if !interface.isAssignableFrom(implementation) =>
                  errors += createMessage(
                    "rest.validator.specification.class.should.implement",
                    property,
                    implementation.getName,
                    interface.getName)
                case Success(_) =>
                case Failure(_) =>
                  errors += createMessage("rest.validator.specification.class.not.found", className, property)
              }
          }

          definitions.foreach {
            case (property, className) =>
              if (Try(classLoader.loadClass(className)).isFailure)
                errors += createMessage("rest.validator.specification.class.not.found", className, property)
          }

        case Failure(_) =>
          errors += createMessage("rest.modules.module.classloader.error", filename)
      }
    }

    errors
  }

  /**
    * Apply method f(moduleType, moduleName, moduleVersion) to this.
    */
  def map[T](f: (String, String, String) => T): T =
    f(specification.moduleType, specification.name, specification.version)

  lazy val signature: String =
    ModuleMetadata.getModuleSignature(specification.moduleType, specification.name, specification.version)
}

object ModuleMetadata {
  def from(fileMetadata: FileMetadataDomain, file: Option[File] = None): ModuleMetadata = {
    val specification = Specification.from(fileMetadata.specification)

    new ModuleMetadata(
      fileMetadata.filename,
      specification,
      file = file,
      name = Option(specification.name),
      version = Option(specification.version),
      length = Option(fileMetadata.length),
      description = Option(specification.description),
      uploadDate = Option(fileMetadata.uploadDate.toString))
  }

  def getModuleSignature(moduleType: String, moduleName: String, moduleVersion: String): String =
    moduleType + "-" + moduleName + "-" + moduleVersion
}
