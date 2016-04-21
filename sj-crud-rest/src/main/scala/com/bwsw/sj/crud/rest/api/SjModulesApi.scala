package com.bwsw.sj.crud.rest.api

import java.io.{FileNotFoundException, File}


import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.{InstanceException, BadRecordWithKey}
import com.bwsw.sj.common.DAL.ConnectionRepository
import com.bwsw.sj.common.entities._
import com.bwsw.sj.common.module.StreamingValidator
import akka.http.scaladsl.model.headers._
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.module.StreamingModuleValidator
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Rest-api for module-jars
  *
  * Created: 08/04/2016
  *
  * @author Kseniya Tomskikh
  */
trait SjModulesApi extends Directives with SjCrudValidator {
  import com.bwsw.sj.common.module.ModuleConstants._

  val modulesApi = {
    pathPrefix("modules") {
      pathEndOrSingleSlash {
        post {
          uploadedFile("jar") {
            case (metadata: FileInfo, file: File) =>
              if (metadata.fileName.endsWith(".jar")) {
                val specification = checkJarFile(file)
                val uploadingFile = new File(metadata.fileName)
                FileUtils.copyFile(file, uploadingFile)
                storage.put(uploadingFile, metadata.fileName, specification, "module")
                complete(HttpEntity(
                  `application/json`,
                  serializer.serialize(Response(200, null, "Jar file has been uploaded"))
                ))
              } else {
                file.delete()
                throw new BadRecordWithKey(s"File: ${metadata.fileName} hasn't the .jar extension", metadata.fileName)
              }
          }
        } ~
        get {
          val files = fileMetadataDAO.retrieveAllByFiletype("module")
          var msg = ""
          if (files.nonEmpty) {
            msg = s"Uploaded modules: ${files.map(
              s => s"${s.metadata("metadata").moduleType} - ${s.metadata("metadata").name} - ${s.metadata("metadata").version}"
            ).mkString(",\n")}"
          } else {
            msg = s"Uploaded modules have not been found "
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(Response(200, null, msg))
          ))
        }
      } ~
      pathPrefix(Segment) { (moduleType: String) =>
        validate(checkModuleType(moduleType), s"Module type $moduleType is not exist") {
          pathPrefix(Segment) { (moduleName: String) =>
            pathPrefix(Segment) { (moduleVersion: String) =>
              val fileMetadata: FileMetadata = fileMetadataDAO.retrieve(moduleName, moduleType, moduleVersion)

              validate(fileMetadata != null, s"Module $moduleType-$moduleName-$moduleVersion not found") {
                val specification = fileMetadata.metadata("metadata")
                val filename = fileMetadata.filename

                pathPrefix("instance") {
                  pathEndOrSingleSlash {
                    post { (ctx: RequestContext) =>
                      val options = deserializeOptions(getEntityFromContext(ctx), moduleType)
                      val validateResult = validateOptions(options, moduleType)
                      val errors = validateResult._1
                      if (errors.isEmpty) {
                        val validatorClassName = specification.validateClass
                        val jarFile = storage.get(filename, s"tmp/$filename")
                        if (jarFile != null && jarFile.exists()) {
                          if (moduleValidate(jarFile, validatorClassName, options.options)) {
                            val nameInstance = saveInstance(options, moduleType, moduleName, moduleVersion, validateResult._2)
                            ctx.complete(HttpEntity(
                              `application/json`,
                              serializer.serialize(Response(200, nameInstance, s"Instance for module $moduleType-$moduleName-$moduleVersion is created"))
                            ))
                          } else {
                            throw new InstanceException(s"Cannot create instance of module. Request has incrorrect options attrubute", s"$moduleType-$moduleName-$moduleVersion")
                          }
                        } else {
                          throw new FileNotFoundException(s"Jar for module $moduleType-$moduleName-$moduleVersion not found in storage")
                        }
                      } else {
                        throw new InstanceException(s"Cannot create instance of module. Errors: ${errors.mkString("\n")}", s"$moduleType-$moduleName-$moduleVersion")
                      }
                    } ~
                    get {
                      val instances = instanceDAO.retrieveByModule(moduleName, moduleVersion, moduleType)
                      var msg = ""
                      if (instances.nonEmpty) {
                        msg = serializer.serialize(instances)
                      } else {
                        msg =  serializer.serialize(Response(200, s"$moduleType-$moduleName-$moduleVersion",
                          s"Instancies for $moduleType-$moduleName-$moduleVersion not found"))
                      }
                      complete(HttpEntity(`application/json`, msg))
                    }
                  } ~
                  path(Segment) { (instanceName: String) =>
                    pathSuffix("start") {
                      get {
                        //todo
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, null, "Ok"))
                        ))
                      }
                    } ~
                    pathSuffix("stop") {
                      get {
                        //todo
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, null, "Ok"))
                        ))
                      }
                    } ~
                    get {
                      val instance = instanceDAO.retrieve(instanceName)
                      instance match {
                        case Some(inst) => complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(inst)
                        ))
                        case None => throw BadRecordWithKey(s"Instance with name $instanceName is not found", instanceName)
                      }
                    } ~
                    delete {
                      if (instanceDAO.delete(instanceName)) {
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, instanceName, s"Instance $instanceName has been deleted"))
                        ))
                      } else {
                        throw new BadRecordWithKey(s"Instance $instanceName hasn't been found", instanceName)
                      }
                    }
                  }
                } ~
                pathSuffix("specification") {
                  pathEndOrSingleSlash {
                    get {
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(specification)
                      ))
                    }
                  }
                } ~
                pathEndOrSingleSlash {
                  get {
                    val jarFile = storage.get(filename, s"tmp/$filename")
                    if (jarFile != null && jarFile.exists()) {
                      complete(HttpResponse(
                        headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                        entity = HttpEntity.Chunked.fromData(`application/java-archive`, Source.file(jarFile))
                      ))
                    } else {
                      throw new BadRecordWithKey(s"Jar '$moduleName' not found", moduleName)
                    }
                  } ~
                  delete {
                    val instances = instanceDAO.retrieveByModule(moduleName, moduleVersion, moduleType)
                    if (instances.nonEmpty) {
                      if (storage.delete(filename)) {
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, s"$moduleType-$moduleName-$moduleVersion", s"Module $moduleName for type $moduleType has been deleted"))
                        ))
                      } else {
                        throw new BadRecordWithKey(s"Module $moduleName hasn't been found", s"$moduleType-$moduleName-$moduleVersion")
                      }
                    } else {
                      throw new BadRecordWithKey(s"Cannot delete module $moduleName. Module has instances", s"$moduleType-$moduleName-$moduleVersion")
                    }
                  }
                }
              }
            }
          } ~
          get {
            val files = fileMetadataDAO.retrieveAllByModuleType(moduleType)
            var msg = ""
            if (files.nonEmpty) {
              msg = s"Uploaded modules for type $moduleType: ${files.map(_.metadata("metadata").name).mkString(",\n")}"
            } else {
              msg = s"Uploaded modules for type $moduleType have not been found "
            }
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, null, msg))
            ))
          }
        }
      } ~
      pathSuffix("instances") {
        get {
          val allInstancies = instanceDAO.retrieveAll()
          if (allInstancies.isEmpty) {
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, null, "Instancies have not been found"))
            ))
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(allInstancies.map(x => ShortInstanceMetadata(x.name, x.moduleType, x.moduleName, x.moduleVersion, x.description, x.status)))
          ))
        }
      }
    }
  }

  /**
    * Deserialization json string to object
    *
    * @param options - json-string
    * @param moduleType - type name of module
    * @return - json as object InstanceMetadata
    */
  def deserializeOptions(options: String, moduleType: String) = {
    if (moduleType.equals(timeWindowedType)) {
      serializer.deserialize[TimeWindowedInstanceMetadata](options)
    } else {
      serializer.deserialize[RegularInstanceMetadata](options)
    }
  }

  /**
    * Validation of options for created module instance
    *
    * @param options - options for instance
    * @param moduleType - type name of module
    * @return - list of errors
    */
  def validateOptions(options: InstanceMetadata, moduleType: String) = {
    val validatorClassName = conf.getString("modules." + moduleType + ".validator-class")
    val validatorClazz = Class.forName(validatorClassName)
    val validator = validatorClazz.newInstance().asInstanceOf[StreamingModuleValidator]
    validator.validate(options)
  }

  /**
    * Save instance of module to mongo db
    *
    * @param parameters - options for instance
    * @param moduleType - type name of module
    * @param moduleName - name of module
    * @param moduleVersion - version of module
    * @return - name of created entity
    */
  def saveInstance(parameters: InstanceMetadata, moduleType: String, moduleName: String, moduleVersion: String, partitionsCount: Map[String, Int]) = {
    parameters.uuid = java.util.UUID.randomUUID().toString
    parameters.moduleName = moduleName
    parameters.moduleVersion = moduleVersion
    parameters.moduleType = moduleType
    parameters.status = ready
    parameters.executionPlan = createExecutionPlan(parameters, partitionsCount)
    instanceDAO.create(parameters)
  }

  /**
    * Create instance of module
    *
    * @param file - jar-file
    * @param validateClassName - validator classname of module
    * @param options - start options for module
    * @return - true, if options for module is valid
    */
  def moduleValidate(file: File, validateClassName: String, options: Map[String, Any]) = {
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validateClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    validator.validate(options)
  }

  /**
    * Create execution plan for instance of module
    * @param instance - instance for module
    * @return - execution plan of instance
    */
  def createExecutionPlan(instance: InstanceMetadata, partitionsCount: Map[String, Int]) = {
    val inputs = instance.inputs.map { input =>
      val mode = getStreamMode(input)
      val name = input.replaceAll("/split|/full", "")
      InputStream(name, mode, partitionsCount(name))
    }
    val tasks = (0 until instance.parallelism)
      .map(x => instance.uuid + "_task" + x)
      .map(x => x -> inputs)

    val executionPlan = mutable.Map[String, Task]()
    val streams = mutable.Map(inputs.map(x => x.name -> StreamProcess(0, x.partitionsCount)).toSeq: _*)

    var tasksNotProcessed = tasks.size
    tasks.foreach { task =>
      val list = task._2.map { inputStream =>
        val stream = streams(inputStream.name)
        val countFreePartitions = stream.countFreePartitions
        val startPartition = stream.currentPartition
        var endPartition = startPartition + countFreePartitions
        inputStream.mode match {
          case "full" => endPartition = startPartition + countFreePartitions
          case "split" =>
            val cntTaskStreamPartitions = countFreePartitions / tasksNotProcessed
            streams.update(inputStream.name, StreamProcess(startPartition + cntTaskStreamPartitions, countFreePartitions - cntTaskStreamPartitions))
            if (Math.abs(cntTaskStreamPartitions - countFreePartitions) >= cntTaskStreamPartitions) {
              endPartition = startPartition + cntTaskStreamPartitions
            }
        }

        inputStream.name -> List(startPartition, endPartition - 1)
      }
      tasksNotProcessed -= 1
      executionPlan.put(task._1, Task(mutable.Map(list.toSeq: _*)))
    }
    ExecutionPlan(executionPlan)
  }

  /**
    * Get mode from stream-name
    * @param name - name of stream
    * @return - mode of stream
    */
  def getStreamMode(name: String) = {
    name.substring(name.lastIndexOf("/") + 1)
  }

  case class InputStream(name: String, mode: String, partitionsCount: Int)

  case class StreamProcess(currentPartition: Int, countFreePartitions: Int)
}
