package com.bwsw.sj.crud.rest.api

import java.io.{IOException, FileNotFoundException, File}

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.{InstanceException, BadRecordWithKey}
import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.module.StreamingValidator
import com.bwsw.sj.crud.rest.entities._
import akka.http.scaladsl.model.headers._
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.module.StreamingModuleValidator
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Rest-api for module-jars
  *
  * Created: 08/04/2016
  *
  * @author Kseniya Tomskikh
  */
trait SjModulesApi extends Directives with SjCrudValidator {
  import scala.collection.JavaConverters._
  import com.bwsw.sj.common.ModuleConstants._
  import com.bwsw.sj.common.JarConstants._
  import com.bwsw.sj.crud.rest.utils.ConvertUtil._

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
                val specification = specificationToSpecificationData(fileSpecification)
                val filename = fileMetadata.filename

                pathPrefix("instance") {
                  pathEndOrSingleSlash {
                    post { (ctx: RequestContext) =>
                      val instanceMetadata = deserializeOptions(getEntityFromContext(ctx), moduleType)
                      val (errors, validatedInstance) = validateOptions(instanceMetadata, moduleType)
                      if (errors.isEmpty) {
                        val validatorClassName = specification.validateClass
                        val jarFile = storage.get(filename, s"tmp/$filename")
                        if (jarFile != null && jarFile.exists()) {
                          if (moduleValidate(jarFile, validatorClassName, validatedInstance.options)) {
                            val nameInstance = saveInstance(validatedInstance, moduleType, moduleName, moduleVersion)
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
                        msg = serializer.serialize(instances.map(i => convertModelInstanceToApiInstance(i)))
                      } else {
                        msg =  serializer.serialize(Response(200, s"$moduleType-$moduleName-$moduleVersion",
                          s"Instances for $moduleType-$moduleName-$moduleVersion not found"))
                      }
                      complete(HttpEntity(`application/json`, msg))
                    }
                  } ~
                  pathPrefix(Segment) { (instanceName: String) =>
                    val instance = instanceDAO.get(instanceName)
                    validate(instance != null, s"Instance for name $instanceName has not been found!") {
                      pathEndOrSingleSlash {
                        get {
                          complete(HttpEntity(
                            `application/json`,
                            serializer.serialize(convertModelInstanceToApiInstance(instance))
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
                      } ~
                      path("start") {
                        pathEndOrSingleSlash {
                          get {
                            complete {
                              startInstance(instance).map[ToResponseMarshallable] {
                                case Right(resp) =>
                                  instance.status = started
                                  instanceDAO.save(instance)
                                  HttpEntity(`application/json`, serializer.serialize(Response(200, null, "Instance is started")))
                                case Left(errorMsg) => BadRequest -> errorMsg
                              }
                            }
                          }
                        }
                      } ~
                      path("stop") {
                        pathEndOrSingleSlash {
                          get {
                            complete {
                              stopInstance(instance).map[ToResponseMarshallable] {
                                case Right(resp) =>
                                  instance.status = stopped
                                  instanceDAO.save(instance)
                                  HttpEntity(`application/json`, serializer.serialize(Response(200, null, "Instance is stopped")))
                                case Left(errorMsg) => BadRequest -> errorMsg
                              }
                            }
                          }
                        }
                      } ~
                      path("destroy") {
                        pathEndOrSingleSlash {
                          get {
                            complete {
                              destroyInstance(instance).map[ToResponseMarshallable] {
                                case Right(resp) =>
                                  instance.status = ready
                                  instanceDAO.save(instance)
                                  HttpEntity(`application/json`, serializer.serialize(Response(200, null, "Instance is destroying")))
                                case Left(errorMsg) => BadRequest -> errorMsg
                              }
                            }
                          }
                        }
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
                    if (instances.isEmpty) {
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
          pathEndOrSingleSlash {
            get {
              val files = fileMetadataDAO.getByParameters(Map("specification.module-type" -> moduleType))
              var msg = ""
              if (files.nonEmpty) {
                msg = s"Uploaded modules for type $moduleType: ${
                  files.map(
                    s => s"${s.specification.name}-${s.specification.version}"
                  ).mkString(",\n")
                }"
              } else {
                msg = s"Uploaded modules for type $moduleType have not been found "
              }
              complete(HttpEntity(
                `application/json`,
                serializer.serialize(Response(200, null, msg))
              ))
            }
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
    if (moduleType.equals(windowedType)) {
      serializer.deserialize[WindowedInstanceMetadata](options)
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
    val validatorClassName = conf.getString(s"modules.$moduleType.validator-class")
    val instanceClassName = conf.getString(s"modules.$moduleType.entity-class")
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[StreamingModuleValidator]
    val instanceClass = Class.forName(instanceClassName)
    validator.validate(options, instanceClass.newInstance().asInstanceOf[RegularInstance])
  }

  /**
    * Save instance of module to db
    *
    * @param instance - entity of instance, which saving to db
    * @param moduleType - type name of module
    * @param moduleName - name of module
    * @param moduleVersion - version of module
    * @return - name of created entity
    */
  def saveInstance(instance: RegularInstance,
                     moduleType: String,
                     moduleName: String,
                     moduleVersion: String) = {
    instance.moduleName = moduleName
    instance.moduleVersion = moduleVersion
    instance.moduleType = moduleType
    instance.status = ready
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
  def moduleValidate(file: File, validateClassName: String, options: String) = {
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validateClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    validator.validate(serializer.deserialize[Map[String, Any]](options))
  }

  /**
    * Starting generators (or scaling) for streams and framework for instance on mesos
    *
    * @param instance - Starting instance
    * @return
    */
  def startInstance(instance: RegularInstance) = {
    val allStreams = instance.inputs.map(_.replaceAll("/split|/full", "")).union(instance.outputs)
      .map(name => streamDAO.get(name))
      .filter(stream => !stream.generator.generatorType.equals("local"))
    val generatorsFuture = Future sequence startGenerators(allStreams.toSet)

    generatorsFuture.flatMap { generators =>
      val restUrl = Uri(s"http://$host:$port/v1/custom/$frameworkJar")
      val taskResponse = getTaskInfo(instance.name)
      taskResponse.flatMap {
        case Right(taskInfo) =>
          if (taskInfo.instances < 1) {
            scaleApplication(instance.name, 1)
          } else {
            logger.info(s"Instance ${instance.name} already started")
            Future.successful(Left(s"Instance ${instance.name} already started"))
          }
        case Left(msg) =>
          getMesosInfo.flatMap {
            case Left(entity) =>
              var applicationEnvs = Map(
                "MONGO_HOST" -> ConnectionConstants.host,
                "MONGO_PORT" -> s"${ConnectionConstants.port}",
                "INSTANCE_ID" -> instance.name,
                "MESOS_MASTER" -> entity)
              if (instance.environments != null) {
                applicationEnvs = applicationEnvs ++ Map(instance.environments.asScala.toList: _*)
              }
              val request = new MarathonRequest(instance.name,
                "java -jar " + frameworkJar + " $PORT",
                1,
                Map(applicationEnvs.toList: _*),
                List(restUrl.toString()))

              startApplication(request)
            case _ => Future.failed(new IOException(s"Cannot starting application"))
          }
      }
    }
  }

  /**
    * Getting mesos master on Zookeeper from marathon
    *
    * @return - Mesos master path
    */
  def getMesosInfo = {
    val marathonUri = Uri(s"$marathonConnect/v2/info")
    Http().singleRequest(HttpRequest(method = GET, uri = marathonUri)).flatMap { response =>
      response.status match {
        case OK =>
          val mesosInfo = serializer.deserialize[Map[String, Any]](getEntityAsString(response.entity))
          val mesosMaster = mesosInfo.get("marathon_config").get.asInstanceOf[Map[String, Any]].get("master").get.asInstanceOf[String]
          Future.successful(Left(mesosMaster))
        case NotFound =>
          Future.successful(Right(NotFound))
        case _ =>
          Future.failed(new IOException(s"Cannot getting info of mesos"))
      }
    }
  }

  /**
    * Starting generators for streams of instance
    * @param streams - Streams
    * @return - Future of started generators
    */
  def startGenerators(streams: Set[SjStream]) = {
    logger.debug("Starting generators")
    streams.map { stream =>
      startGenerator(stream)
    }
  }

  /**
    * Starting transaction generator for stream on mesos
    *
    * @param stream - Stream for running generator
    * @return - Future with response from request to marathon
    */
  def startGenerator(stream: SjStream) = {
    val zkService = stream.generator.service.asInstanceOf[ZKService]
    val generatorProvider = zkService.provider
    var prefix = zkService.namespace
    var taskId = ""
    if (stream.generator.generatorType.equals("per-stream")) {
      prefix += s"/${stream.name}"
      taskId = s"${stream.generator.service.name}-${stream.name}-tg"
    } else {
      prefix += "/global"
      taskId = s"${stream.generator.service.name}-global-tg"
    }

    val restUrl = Uri(s"http://$host:$port/v1/custom/$transactionGeneratorJar")

    val marathonRequest = MarathonRequest(taskId.replaceAll("_", "-"),
      "java -jar " + transactionGeneratorJar + " $PORT",
      stream.generator.instanceCount,
      Map("ZK_SERVERS" -> generatorProvider.hosts.mkString(";"), "PREFIX" -> prefix),
      List(restUrl.toString()))

    val taskResponse = getTaskInfo(marathonRequest.id)
    taskResponse.flatMap {
      case Right(taskInfo) =>
        if (taskInfo.instances < marathonRequest.instances) {
          scaleApplication(marathonRequest.id, marathonRequest.instances)
        } else {
          logger.debug(s"Generator ${marathonRequest.id} already started")
          Future.successful(Left(s"Generator ${marathonRequest.id} already started"))
        }
      case Left(msg) => startApplication(marathonRequest)
    }
  }

  /**
    * Starting application on mesos
    *
    * @param request - Marathon request entity (json)
    * @return Response from marathon or error text
    */
  def startApplication(request: MarathonRequest) = {
    val marathonUri = Uri(s"$marathonConnect/v2/apps")
    Http().singleRequest(HttpRequest(method = POST, uri = marathonUri,
      entity = HttpEntity(ContentTypes.`application/json`, serializer.serialize(request))
    )).flatMap { response =>
      response.status match {
        case Created =>
          logger.debug(s"Application ${request.id} is started")
          Future.successful(Right(response))
        case _ =>
          logger.debug(s"Cannot starting application by id: ${request.id}")
          Future.successful(Left(s"Cannot starting application by id: ${request.id}"))
      }
    }
  }

  /**
    * Getting information about application on mesos
    *
    * @param taskId - Application ID on mesos
    * @return - Application info or error
    */
  private def getTaskInfo(taskId: String) = {
    serializer.setIgnoreUnknown(true)
    val marathonUri = Uri(s"$marathonConnect/v2/apps/$taskId")
    Http().singleRequest(HttpRequest(method = GET, uri = marathonUri)).flatMap { response =>
      response.status match {
        case OK => Future.successful(Right(serializer.deserialize[MarathonRequest](getEntityAsString(response.entity))))
        case NotFound => Future.successful(Left(s"Application by id: $taskId i snot started"))
        case _ => Future.failed(new IOException(s"Cannot getting info for task by id: $taskId"))
      }
    }
  }

  /**
    * Scale application on mesos
    *
    * @param taskId - Application ID on marathon for scaling
    * @param count - New count of instances of application
    * @return - Message about successful scaling
    */
  private def scaleApplication(taskId: String, count: Int) = {
    val marathonUri = Uri(s"$marathonConnect/v2/apps/$taskId?force=true")
    Http().singleRequest(HttpRequest(method = PUT, uri = marathonUri,
      entity = HttpEntity(ContentTypes.`application/json`, serializer.serialize(Map("instances" -> count)))
    )).flatMap { response =>
      response.status match {
        case OK =>
          logger.debug(s"Application $taskId is scaled")
          Future.successful(Right(s"Application is scaled"))
        case BadRequest =>
          logger.debug(s"Application $taskId cannot scaling")
          Future.successful(Left(s"Application cannot scaling"))
        case _ => Future.failed(new IOException(s"Cannot scaling to application by id: $taskId"))
      }
    }
  }

  /**
    * Suspend application on mesos
    *
    * @param taskId - Application ID on mesos for suspending
    * @return - Message about successful scaling
    */
  private def descaleApplication(taskId: String) = {
    logger.debug(s"Descaling application $taskId")
    scaleApplication(taskId, 0)
  }


  /**
    * Stopping instance application on mesos
    *
    * @param instance - Instance for stopping
    * @return - Message about successful stopping
    */
  def stopInstance(instance: RegularInstance) = {
    logger.debug("Stopping generators")
    stopGenerators(instance)
    descaleApplication(instance.name)
  }

  /**
    * Stopping all running generators for streams of instance,
    * if generators is not using any streams of started instances
    *
    * @param instance - Instance for stopping
    * @return - Response from marathon
    */
  def stopGenerators(instance: RegularInstance) = {
    val allStreams = instance.inputs.map(_.replaceAll("/split|/full", "")).union(instance.outputs).map(streamDAO.get)
    val startedInstances = instanceDAO.getByParameters(Map("status" -> started))
    allStreams.filter { stream: SjStream =>
      !stream.generator.generatorType.equals("local") &&
        !startedInstances.exists(instance => instance.inputs.contains(stream.name) ||
          instance.outputs.contains(stream.name))
    }.map { stream =>
      var taskId = ""
      if (stream.generator.generatorType.equals("per-stream")) {
        taskId = s"${stream.generator.service.name}-${stream.name}-tg"
      } else {
        taskId = s"${stream.generator.service.name}-global-tg"
      }
      stopApplication(taskId.replaceAll("_", "-"))
    }
  }

  /**
    * Stopping stream generator on mesos
    *
    * @param taskId - Generator task id on mesos
    * @return
    */
  private def stopApplication(taskId: String) = {
    val marathonUri = Uri(s"$marathonConnect/v2/apps/$taskId")
    Http().singleRequest(HttpRequest(method = DELETE, uri = marathonUri))
  }

  /**
    * Destroying application on mesos
    *
    * @param instance - Instanse for destroying
    * @return - Message of destroying instance
    */
  def destroyInstance(instance: RegularInstance) = {
    stopApplication(instance.name).flatMap { response =>
      response.status match {
        case OK => Future.successful(Right(s"Application destroying"))
        case _ => Future.successful(Left(s"Application cannot destroy"))
      }
    }
  }

}
