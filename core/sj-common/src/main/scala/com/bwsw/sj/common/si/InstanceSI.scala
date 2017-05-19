package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, Specification}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

class InstanceSI {
  private val entityRepository: GenericMongoRepository[InstanceDomain] = ConnectionRepository.getInstanceRepository
  private val storage = ConnectionRepository.getFileStorage

  def create(instance: Instance, moduleMetadata: ModuleMetadata): Either[ArrayBuffer[String], Boolean] = {
    val instancePassedValidation = validateInstance(moduleMetadata.specification, moduleMetadata.filename, instance)

    if (instancePassedValidation.result) {
      instance.createStreams()
      entityRepository.save(instance.to)
      Right(true)
    } else {
      Left(instancePassedValidation.errors)
    }
  }

  def getAll: mutable.Buffer[Instance] =
    entityRepository.getAll.map(Instance.from)

  def getByModule(moduleType: String, moduleName: String, moduleVersion: String): mutable.Buffer[Instance] = {
    entityRepository.getByParameters(
      Map(
        "module-name" -> moduleName,
        "module-type" -> moduleType,
        "module-version" -> moduleVersion))
      .map(Instance.from)
  }

  def get(name: String): Option[Instance] =
    entityRepository.get(name).map(Instance.from)

  def delete(name: String): Either[String, Boolean] = {
    val instance = get(name).get
    instance.status match {
      case `ready` =>
        entityRepository.delete(name: String)
        Right(true)
      case `stopped` | `failed` | `error` =>
        Right(false)
      case _ =>
        Left(createMessage("rest.modules.instances.instance.cannot.delete", name))
    }
  }

  def canStart(instance: Instance): Boolean =
    Set(ready, stopped, failed).contains(instance.status)

  def canStop(instance: Instance): Boolean =
    instance.status == started


  private def validateInstance(specification: Specification, filename: String, instance: Instance): ValidationInfo = {
    val validatorClassName = specification.validatorClass
    val file = storage.get(filename, s"tmp/$filename")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validatorClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    val optionsValidationInfo = validator.validate(instance)
    val instanceValidationInfo = validator.validate(instance.options)

    ValidationInfo(
      optionsValidationInfo.result && instanceValidationInfo.result,
      optionsValidationInfo.errors ++= instanceValidationInfo.errors)
  }
}
