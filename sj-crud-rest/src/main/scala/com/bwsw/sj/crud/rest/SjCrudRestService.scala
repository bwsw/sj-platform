package com.bwsw.sj.crud.rest

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.event.Logging.LogLevel
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{LoggingMagnet, DebuggingDirectives, LogEntry}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.ConfigLoader
import com.bwsw.sj.common.DAL.ConnectionRepository
import com.typesafe.config.Config

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

  val conf: Config = ConfigLoader.load()

  implicit val system = ActorSystem("sj-crud-rest-server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher //for work with future

  val host = System.getenv("HOST")
  val port = System.getenv("PORT").toInt

  val serializer = new JsonSerializer()
  val storage = ConnectionRepository.getFileStorage
  val fileMetadataDAO = ConnectionRepository.getFileMetadataDAO
  val instanceDAO = ConnectionRepository.getInstanceDAO

  val routeLogged = logRequestResult(Logging.InfoLevel, route())

  val serverBinding: Future[ServerBinding] = Http().bindAndHandle(routeLogged, interface = host, port = port)

  serverBinding onFailure {
    case ex: Exception =>
      println("Failed to bind to {}:{}!", host, port)
  }

  println(s"Server online at http://$host:$port/\nPress ENTER to stop...")
  StdIn.readLine()
  serverBinding.flatMap(_.unbind())
    .onComplete(_ => system.shutdown())

}
