package com.bwsw.sj.crud.rest.routes

import java.io.File

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.sj.crud.rest.controller.{CustomFilesController, CustomJarsController}
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

/**
  * Rest-api for sj-platform executive units and custom files
  *
  * @author Kseniya Tomskikh
  */
trait SjCustomRoute extends Directives with SjCrudValidator {
  private val customJarsController = new CustomJarsController()
  private val customFilesController = new CustomFilesController()

  val customRoute = {
    pathPrefix("custom") {
      pathPrefix("jars") {
        pathPrefix(Segment) { (name: String) =>
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(customJarsController.get(name)))
            }
          } ~
            delete {
              complete(restResponseToHttpResponse(customJarsController.delete(name)))
            } ~
            pathSuffix(Segment) { (version: String) =>
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(customJarsController.getBy(name, version)))
                } ~
                  delete {
                    complete(restResponseToHttpResponse(customJarsController.deleteBy(name, version)))
                  }
              }
            }
        } ~
          pathEndOrSingleSlash {
            post {
              uploadedFile("jar") {
                case (metadata: FileInfo, file: File) =>
                  val fileMetadataApi = new FileMetadataApi(filename = metadata.fileName, file = Some(file))
                  complete(restResponseToHttpResponse(customJarsController.create(fileMetadataApi)))
              }
            } ~
              get {
                complete(restResponseToHttpResponse(customJarsController.getAll()))
              }
          }
      } ~
        pathPrefix("files") {
          pathEndOrSingleSlash {
            post {
              entity(as[Multipart.FormData]) { formData =>
                val fileMetadataApi = new FileMetadataApi(formData = Some(formData))
                complete(restResponseToHttpResponse(customFilesController.create(fileMetadataApi)))
              }
            } ~
              get {
                complete(restResponseToHttpResponse(customFilesController.getAll()))
              }
          } ~
            pathPrefix(Segment) { (filename: String) =>
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(customFilesController.get(filename)))
                } ~
                  delete {
                    complete(restResponseToHttpResponse(customFilesController.delete(filename)))
                  }
              }
            }
        }
    }
  }
}
