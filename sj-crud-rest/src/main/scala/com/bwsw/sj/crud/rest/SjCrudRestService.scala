package com.bwsw.sj.crud.rest

import akka.actor.ActorSystem
import akka.event.Logging.LogLevel
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

import scala.concurrent.Future
import scala.io.StdIn

/**
 * Run object of CRUD Rest-API
 * Created: 06/04/2016
 * @author Kseniya Tomskikh
 */
object SjCrudRestService extends App with SjCrudRouter {

  def logRequestResult(level: LogLevel, route: Route) = {
    def getRequestEntityAsString(logger: LoggingAdapter)(req: HttpRequest)(res: Any): Unit = {
      val entry = res match {
        case Complete(resp) =>
          entityAsString(resp.entity).map(data ⇒ LogEntry(s"${req.method} ${req.uri}: ${resp.status} \n entity: $data", level))
        case other =>
          Future.successful(LogEntry(s"$other", level))
      }
      entry.map(_.logTo(logger))
    }
    DebuggingDirectives.logRequestResult(LoggingMagnet(log => getRequestEntityAsString(log)))(route)
  }

  def entityAsString(entity: HttpEntity): Future[String] = {
    entity.dataBytes
      .map(_.decodeString(entity.contentType().charset().value))
      .runWith(Sink.head)
  }

  implicit val system = ActorSystem("sj-crud-rest-server")
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.dispatcher //for work with future

  val restHost = System.getenv("CRUD_REST_HOST")
  val restPort = System.getenv("CRUD_REST_PORT").toInt

  val serializer = new JsonSerializer()
  val storage = ConnectionRepository.getFileStorage
  val fileMetadataDAO = ConnectionRepository.getFileMetadataService
  val instanceDAO = ConnectionRepository.getInstanceService
  val serviceDAO = ConnectionRepository.getServiceManager
  val providerDAO = ConnectionRepository.getProviderService
  val streamDAO = ConnectionRepository.getStreamService
  val configService = ConnectionRepository.getConfigService

  val routeLogged = logRequestResult(Logging.InfoLevel, route())
  val logger = Logging(system, getClass)

  putRestSettingsToConfigFile()

  val serverBinding: Future[ServerBinding] = Http().bindAndHandle(routeLogged, interface = restHost, port = restPort)

  serverBinding onFailure {
    case ex: Exception =>
      println("Failed to bind to {}:{}!", restHost, restPort)
  }

  println(s"Server online at http://$restHost:$restPort/\nPress ENTER to stop...")
  StdIn.readLine()
  serverBinding.flatMap(_.unbind())
    .onComplete(_ => system.terminate())

  private def putRestSettingsToConfigFile() = {
    configService.save(new ConfigSetting(ConfigConstants.hostOfCrudRestTag, restHost))
    configService.save(new ConfigSetting(ConfigConstants.portOfCrudRestTag, restPort.toString))
  }
}
