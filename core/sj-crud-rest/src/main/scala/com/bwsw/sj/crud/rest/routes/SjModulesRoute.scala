package com.bwsw.sj.crud.rest.routes

import java.io.File

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.sj.common.si.JsonValidator
import com.bwsw.sj.crud.rest.controller.{InstanceController, ModuleController}
import com.bwsw.sj.crud.rest.model.module.ModuleMetadataApi
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjModulesRoute extends Directives with SjCrudValidator with JsonValidator {

  private val moduleController = new ModuleController
  private val instanceController = new InstanceController

  val modulesRoute =
    pathPrefix("modules") {
      pathEndOrSingleSlash {
        post {
          uploadedFile("jar") {
            case (metadata: FileInfo, file: File) =>
              val moduleMetadataApi = new ModuleMetadataApi(metadata.fileName, file)
              complete(restResponseToHttpResponse(moduleController.create(moduleMetadataApi)))
          }
        } ~
          get {
            complete(restResponseToHttpResponse(moduleController.getAll))
          }
      } ~
        pathPrefix("instances") {
          get {
            complete(restResponseToHttpResponse(instanceController.getAll))
          }
        } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(moduleController.getAllTypes))
            }
          }
        } ~
        pathPrefix(Segment) { (moduleType: String) =>
          pathPrefix(Segment) { (moduleName: String) =>
            pathPrefix(Segment) { (moduleVersion: String) =>
              pathPrefix("instance") {
                pathEndOrSingleSlash {
                  post {
                    entity(as[String]) { entity =>
                      complete(
                        restResponseToHttpResponse(
                          instanceController.create(entity, moduleType, moduleName, moduleVersion)))
                    }
                  } ~
                    get {
                      complete(
                        restResponseToHttpResponse(
                          instanceController.getByModule(moduleType, moduleName, moduleVersion)))
                    }
                } ~
                  pathPrefix(Segment) { (instanceName: String) =>
                    pathEndOrSingleSlash {
                      get {
                        complete(
                          restResponseToHttpResponse(
                            instanceController.get(moduleType, moduleName, moduleVersion, instanceName)))
                      } ~
                        complete(
                          restResponseToHttpResponse(
                            instanceController.delete(moduleType, moduleName, moduleVersion, instanceName)))
                    } ~
                      path("start") {
                        pathEndOrSingleSlash {
                          complete(
                            restResponseToHttpResponse(
                              instanceController.start(moduleType, moduleName, moduleVersion, instanceName)))
                        }
                      } ~
                      path("stop") {
                        pathEndOrSingleSlash {
                          complete(
                            restResponseToHttpResponse(
                              instanceController.stop(moduleType, moduleName, moduleVersion, instanceName)))
                        }
                      } ~
                      pathPrefix("tasks") {
                        pathEndOrSingleSlash {
                          complete(
                            restResponseToHttpResponse(
                              instanceController.tasks(moduleType, moduleName, moduleVersion, instanceName)))
                        }
                      }
                  }
              } ~
                pathPrefix("specification") {
                  pathEndOrSingleSlash {
                    get {
                      complete(
                        restResponseToHttpResponse(
                          moduleController.getSpecification(moduleType, moduleName, moduleVersion)))
                    }
                  }
                } ~
                pathEndOrSingleSlash {
                  get {
                    complete(
                      restResponseToHttpResponse(
                        moduleController.get(moduleType, moduleName, moduleVersion)))
                  } ~
                    delete {
                      complete(
                        restResponseToHttpResponse(
                          moduleController.delete(moduleType, moduleName, moduleVersion)))
                    }
                } ~
                pathPrefix("related") {
                  pathEndOrSingleSlash {
                    get {
                      complete(
                        restResponseToHttpResponse(
                          moduleController.getRelated(moduleType, moduleName, moduleVersion)))
                    }
                  }
                }
            }
          } ~
            pathEndOrSingleSlash {
              get {
                complete(restResponseToHttpResponse(moduleController.getByType(moduleType)))
              }
            }
        }
    }
}
