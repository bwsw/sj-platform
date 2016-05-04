package com.bwsw.sj.crud.rest.api

import java.io.{FileNotFoundException, File}
import java.net.URI
import java.util


import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.{InstanceException, BadRecordWithKey}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.common.module.StreamingValidator
import akka.http.scaladsl.model.headers._
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.module.StreamingModuleValidator
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._

import scala.collection.mutable
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
  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

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
          val files = fileMetadataDAO.getByParameters(Map("filetype" -> "module"))
          var msg = ""
          if (files.nonEmpty) {
            msg = s"Uploaded modules: ${files.map(
              s => s"${s.specification.moduleType} - ${s.specification.name} - ${s.specification.version}"
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
              val fileMetadatas = fileMetadataDAO.getByParameters(Map("specification.name" -> moduleName,
                "specification.module-type" -> moduleType,
                "specification.version" -> moduleVersion)
              )
              val fileMetadata = fileMetadatas.head

              validate(fileMetadata != null, s"Module $moduleType-$moduleName-$moduleVersion not found") {
                val fileSpecification = fileMetadata.specification
                val specification = Specification(fileSpecification.name,
                  fileSpecification.description,
                  fileSpecification.version,
                  fileSpecification.author,
                  fileSpecification.license,
                  Map("cardinality" -> fileSpecification.inputs.cardinality,
                    "types" -> fileSpecification.inputs.types),
                  Map("cardinality" -> fileSpecification.outputs.cardinality,
                    "types" -> fileSpecification.outputs.types),
                  fileSpecification.moduleType,
                  fileSpecification.engine,
                  serializer.deserialize[Map[String, Any]](fileSpecification.options),
                  fileSpecification.validateClass,
                  fileSpecification.executorClass)

                val filename = fileMetadata.filename

                pathPrefix("instance") {
                  pathEndOrSingleSlash {
                    post { (ctx: RequestContext) =>
                      val instanceMetadata = deserializeOptions(getEntityFromContext(ctx), moduleType)
                      val (errors, partitions, validatedInstance) = validateOptions(instanceMetadata, moduleType)
                      if (errors.isEmpty) {
                        val validatorClassName = specification.validateClass
                        val jarFile = storage.get(filename, s"tmp/$filename")
                        if (jarFile != null && jarFile.exists()) {
                          if (moduleValidate(jarFile, validatorClassName, validatedInstance.options)) {
                            val nameInstance = createInstance(validatedInstance, moduleType, moduleName, moduleVersion, partitions)
                            ctx.complete(HttpEntity(
                              `application/json`,
                              serializer.serialize(Response(200, nameInstance, s"Instance for module $moduleType-$moduleName-$moduleVersion is created"))
                            ))
                          } else {
                            throw new InstanceException(s"Cannot create instance of module. Request has incrorrect options attrubute",
                              s"$moduleType-$moduleName-$moduleVersion")
                          }
                        } else {
                          throw new FileNotFoundException(s"Jar for module $moduleType-$moduleName-$moduleVersion not found in storage")
                        }
                      } else {
                        throw new InstanceException(s"Cannot create instance of module. Errors: ${errors.mkString("\n")}",
                          s"$moduleType-$moduleName-$moduleVersion")
                      }
                    } ~
                    get {
                      val instances = instanceDAO.getByParameters(Map("module-name" -> moduleName,
                        "module-type" -> moduleType,
                        "module-version" -> moduleVersion)
                      )
                      var msg = ""
                      if (instances.nonEmpty) {
                        msg = serializer.serialize(instances.map(i => convertToApiInstance(i)))
                      } else {
                        msg =  serializer.serialize(Response(200, s"$moduleType-$moduleName-$moduleVersion",
                          s"Instances for $moduleType-$moduleName-$moduleVersion not found"))
                      }
                      complete(HttpEntity(`application/json`, msg))
                    }
                  } ~
                  path(Segment) { (instanceName: String) =>
                    val instance = instanceDAO.get(instanceName)
                    validate(instance != null, s"Instance for name $instanceName has not been found!") {
                      pathSuffix("start") {
                        get {
                          //todo
                          startInstance(instance)
                          complete(HttpEntity(
                            `application/json`,
                            serializer.serialize(Response(200, null, "Ok"))
                          ))
                        }
                      } ~
                      pathSuffix("stop") {
                        get {
                          //todo
                          stopInstance(instance)
                          complete(HttpEntity(
                            `application/json`,
                            serializer.serialize(Response(200, null, "Ok"))
                          ))
                        }
                      } ~
                      get {
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(convertToApiInstance(instance))
                        ))
                      } ~
                      delete {
                        //todo add checking
                        instanceDAO.delete(instanceName)
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, instanceName, s"Instance $instanceName has been deleted"))
                        ))
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
                      throw new BadRecordWithKey(s"Jar '$moduleType-$moduleName-$moduleVersion' not found",
                        s"$moduleType - $moduleName - $moduleVersion")
                    }
                  } ~
                  delete {
                    val instances = instanceDAO.getByParameters(Map("module-name" -> moduleName,
                      "module-type" -> moduleType,
                      "module-version" -> moduleVersion)
                    )
                    if (instances.nonEmpty) {
                      if (storage.delete(filename)) {
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, s"$moduleType-$moduleName-$moduleVersion",
                            s"Module $moduleName-$moduleVersion for type $moduleType has been deleted"))
                        ))
                      } else {
                        throw new BadRecordWithKey(s"Module $moduleType-$moduleName-$moduleVersion hasn't been found",
                          s"$moduleType-$moduleName-$moduleVersion")
                      }
                    } else {
                      throw new BadRecordWithKey(s"Cannot delete module $moduleType-$moduleName-$moduleVersion. Module has instances",
                        s"$moduleType-$moduleName-$moduleVersion")
                    }
                  }
                }
              }
            }
          } ~
          get {
            val files = fileMetadataDAO.getByParameters(Map("specification.module-type" -> moduleType))
            var msg = ""
            if (files.nonEmpty) {
              msg = s"Uploaded modules for type $moduleType: ${files.map(
                s => s"${s.specification.name}-${s.specification.version}"
              ).mkString(",\n")}"
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
          val allInstances = instanceDAO.getAll
          if (allInstances.isEmpty) {
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, null, "Instances have not been found"))
            ))
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(allInstances.map(x => ShortInstanceMetadata(x.name,
              x.moduleType,
              x.moduleName,
              x.moduleVersion,
              x.description,
              x.status)))
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
      serializer.deserialize[InstanceMetadata](options)
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


  def createInstance(parameters: InstanceMetadata,
                     moduleType: String,
                     moduleName: String,
                     moduleVersion: String,
                     partitionsCount: Map[String, Int]) = {
    val executionPlan = createExecutionPlan(parameters, partitionsCount)
    if (moduleType.equals(timeWindowedType)) {
      val instance = convertToModelInstance(new TimeWindowedInstance, parameters).asInstanceOf[TimeWindowedInstance]
      val twParameters = parameters.asInstanceOf[TimeWindowedInstanceMetadata]
      instance.timeWindowed = twParameters.timeWindowed
      instance.windowFullMax = twParameters.windowFullMax
      saveInstance(instance, moduleType, moduleName, moduleVersion, executionPlan)
    } else {
      val instance = convertToModelInstance(new RegularInstance, parameters)
      saveInstance(instance, moduleType, moduleName, moduleVersion, executionPlan)
    }
  }

  /**
    * Save instance of module to db
    *
    * @param instance - entity of instance, which saving to db
    * @param moduleType - type name of module
    * @param moduleName - name of module
    * @param moduleVersion - version of module
    * @param executionPlan - execution plan for instance of module
    * @return - name of created entity
    */
  def saveInstance(instance: RegularInstance,
                     moduleType: String,
                     moduleName: String,
                     moduleVersion: String,
                     executionPlan: ExecutionPlan) = {
    instance.moduleName = moduleName
    instance.moduleVersion = moduleVersion
    instance.moduleType = moduleType
    instance.status = ready
    instance.executionPlan = executionPlan
    instanceDAO.save(instance)
    instance.name
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
    *
    * @param instance - instance for module
    * @return - execution plan of instance
    */
  def createExecutionPlan(instance: InstanceMetadata, partitionsCount: Map[String, Int]) = {
    val inputs = instance.inputs.map { input =>
      val mode = getStreamMode(input)
      val name = input.replaceAll("/split|/full", "")
      InputStream(name, mode, partitionsCount(name))
    }
    val minPartitionCount = partitionsCount.values.min
    var parallelism = 0
    instance.parallelism match {
      case i: Int => parallelism = i
      case s: String => parallelism = minPartitionCount
    }
    val tasks = (0 until parallelism)
      .map(x => instance.name + "_task" + x)
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

        inputStream.name -> Array(startPartition, endPartition - 1)
      }
      tasksNotProcessed -= 1
      val planTask = new Task
      planTask.inputs = mapAsJavaMap(Map(list.toSeq: _*))
      executionPlan.put(task._1, planTask)
    }
    val execPlan = new ExecutionPlan
    execPlan.tasks = mapAsJavaMap(executionPlan)
    execPlan
  }

  /**
    * Get mode from stream-name
    *
    * @param name - name of stream
    * @return - mode of stream
    */
  def getStreamMode(name: String) = {
    name.substring(name.lastIndexOf("/") + 1)
  }

  case class InputStream(name: String, mode: String, partitionsCount: Int)

  case class StreamProcess(currentPartition: Int, countFreePartitions: Int)

  case class Generator(generatorType: String, zkServers: Array[String], prefix: String, count: Int)

  def startInstance(instance: RegularInstance) = {

    instance.inputs.map(_.replaceAll("/split|/full", "")).foreach { streamName =>
      val stream = streamDAO.get(streamName)
      if (!stream.generator.generatorType.equals("local")) {
        startGenerator(stream)
      }
    }

    //todo start instance

  }

  def startGenerator(stream: SjStream) = {
    val generatorService = stream.generator.service
    var zkService: ZKService = null
    generatorService.serviceType match {
      case "ZKCoord" => zkService = generatorService.asInstanceOf[ZKService]
      case _ => throw new Exception("Unknown")
    }
    val generatorProvider = generatorService.provider
    var prefix = zkService.namespace
    if (stream.generator.generatorType.equals("per-stream")) {
      prefix += s"/${stream.name}"
    } else {
      prefix += "/global"
    }
    val generator = Generator(stream.generator.generatorType, generatorProvider.hosts, prefix, stream.generator.instanceCount)

    val marathonRequest = MarathonRequest(s"task_tg_${stream.name}",
      "java -jar sj-transaction-generator-assembly-1.0.jar $PORT",
      generator.count,
      Map("ZK_SERVERS" -> generator.zkServers.mkString(";"), "PREFIX" -> prefix),
      List(s"http://$host:$port/v1/custom/sj-transaction-generator-assembly-1.0.jar"))

    startApplication(marathonRequest)


  }

  private def startApplication(request: MarathonRequest) = {
    val uri = Uri()
  }

  //todo stop
  def stopInstance(instance: RegularInstance) = {

  }

  def convertToApiInstance(instance: RegularInstance) = {
    instance match {
      case timeWindowedInstance: TimeWindowedInstance =>
        val apiInstance = convert(new TimeWindowedInstanceMetadata, instance).asInstanceOf[TimeWindowedInstanceMetadata]
        apiInstance.timeWindowed = timeWindowedInstance.timeWindowed
        apiInstance.windowFullMax = timeWindowedInstance.windowFullMax
        apiInstance
      case _ => convert(new InstanceMetadata, instance)
    }
  }

  /**
    * Convert model instance object to API instance
    *
    * @param instance - object of model instance
    * @return - API instance object
    */
  def convert(apiInstance: InstanceMetadata, instance: RegularInstance): InstanceMetadata = {
    val executionPlan = Map(
      "tasks" -> instance.executionPlan.tasks.map(t => t._1 -> Map("inputs" -> t._2.inputs))
    )
    apiInstance.status = instance.status
    apiInstance.name = instance.name
    apiInstance.description = instance.description
    apiInstance.inputs = instance.inputs
    apiInstance.outputs = instance.outputs
    apiInstance.checkpointMode = instance.checkpointMode
    apiInstance.checkpointInterval = instance.checkpointInterval
    apiInstance.stateManagement = instance.stateManagement
    apiInstance.stateFullCheckpoint = instance.stateFullCheckpoint
    apiInstance.parallelism = instance.parallelism
    apiInstance.options = serializer.deserialize[Map[String, Any]](instance.options)
    apiInstance.startFrom = instance.startFrom
    apiInstance.perTaskCores = instance.perTaskCores
    apiInstance.perTaskRam = instance.perTaskRam
    apiInstance.jvmOptions = Map(instance.jvmOptions.asScala.toList: _*)
    apiInstance.tags = instance.tags
    apiInstance.idle = instance.idle
    apiInstance.executionPlan = executionPlan
    apiInstance
  }

  /**
    * Convert api instance to db-model instance
    *
    * @param modelInstance - dst object of model instance
    * @param apiInstance - api object of instance
    * @return - object of model instance
    */
  def convertToModelInstance(modelInstance: RegularInstance, apiInstance: InstanceMetadata) = {
    modelInstance.name = apiInstance.name
    modelInstance.description = apiInstance.description
    modelInstance.inputs = apiInstance.inputs
    modelInstance.outputs = apiInstance.outputs
    modelInstance.checkpointMode = apiInstance.checkpointMode
    modelInstance.checkpointInterval = apiInstance.checkpointInterval
    modelInstance.stateFullCheckpoint = apiInstance.stateFullCheckpoint
    modelInstance.stateManagement = apiInstance.stateManagement
    modelInstance.parallelism = apiInstance.parallelism.asInstanceOf[Int]
    modelInstance.options = serializer.serialize(apiInstance.options)
    modelInstance.startFrom = apiInstance.startFrom
    modelInstance.perTaskCores = apiInstance.perTaskCores
    modelInstance.perTaskRam = apiInstance.perTaskRam
    modelInstance.jvmOptions = mapAsJavaMap(apiInstance.jvmOptions)
    modelInstance.tags = apiInstance.tags
    modelInstance.idle = apiInstance.idle
    modelInstance
  }
}
