package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, Specification}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

class InstanceSI(implicit injector: Injector) {
  private val connectionRepository = inject[ConnectionRepository]
  private val entityRepository: GenericMongoRepository[InstanceDomain] = connectionRepository.getInstanceRepository
  private val storage = connectionRepository.getFileStorage

  def create(instance: Instance, moduleMetadata: ModuleMetadata): CreationResult = {
    val instancePassedValidation = validateInstance(moduleMetadata.specification, moduleMetadata.filename, instance)

    if (instancePassedValidation.result) {
      instance.createStreams()
      entityRepository.save(instance.to)

      Created
    } else {
      NotCreated(instancePassedValidation.errors)
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

  def delete(name: String): DeletionResult = {
    entityRepository.get(name) match {
      case Some(instance) =>
        instance.status match {
          case `ready` =>
            entityRepository.delete(name: String)

            Deleted
          case `stopped` | `failed` | `error` =>
            WillBeDeleted(Instance.from(instance))
          case _ =>
            DeletionError(createMessage("rest.modules.instances.instance.cannot.delete", name))
        }

      case None =>
        EntityNotFound
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

case class WillBeDeleted(instance: Instance) extends DeletionResult
