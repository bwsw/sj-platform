package com.bwsw.sj.crud.rest.routes

import java.io.File

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.FileIO
import com.bwsw.sj.crud.rest.controller.{CustomFilesController, CustomJarsController}
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

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
            } ~
            delete {
              complete(restResponseToHttpResponse(customJarsController.delete(name)))
            }
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
                  val fileMetadataApi = new FileMetadataApi(filename = Some(metadata.fileName), file = Some(file))
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
                var filename: Option[String] = None
                val file = File.createTempFile("dummy", "")
                val parts: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {

                  case b: BodyPart if b.name == "file" =>
                    filename = b.filename
                    b.entity.dataBytes.runWith(FileIO.toPath(file.toPath)).map(_ => b.name -> file)

                  case b: BodyPart =>
                    b.toStrict(2.seconds).map(strict => b.name -> strict.entity.data.utf8String)

                }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)

                onComplete(parts) {
                  case Success(allParts) =>
                    val fileMetadataApi = new FileMetadataApi(filename = filename, customFileParts = allParts)

                    complete(restResponseToHttpResponse(customFilesController.create(fileMetadataApi)))
                  case Failure(throwable) =>
                    file.delete()
                    throw throwable
                }
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
