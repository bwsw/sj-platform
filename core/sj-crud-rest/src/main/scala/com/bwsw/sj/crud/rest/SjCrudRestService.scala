package com.bwsw.sj.crud.rest

import akka.event.Logging.LogLevel
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.scaladsl.Sink
import com.bwsw.sj.common.SjModule
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.crud.rest.instance.InstanceStopper
import com.bwsw.sj.crud.rest.utils.RestLiterals
import com.typesafe.config.ConfigFactory
import scaldi.Injectable.inject
import scaldi.{Injector, Module}

import scala.concurrent.Future

/**
  * Run object of CRUD Rest-API
  *
  * @author Kseniya Tomskikh
  */
object SjCrudRestService extends {
  implicit val module: Module = SjModule.module
  override implicit val injector: Injector = SjModule.injector
} with App with SjCrudRestApi {

  val connectionRepository = inject[ConnectionRepository]
  private val config = ConfigFactory.load()
  private val restHost = config.getString(RestLiterals.hostConfig)
  private val restPort = config.getInt(RestLiterals.portConfig)
  private val instanceDAO = connectionRepository.getInstanceRepository
  private val routeLogged = logRequestResult(Logging.InfoLevel, route())
  private val logger = Logging(system, getClass)

  stopInstances()

  val serverBinding: Future[ServerBinding] = Http().bindAndHandle(routeLogged, interface = restHost, port = restPort)
  serverBinding.failed.foreach(_ => logger.error("Failed to bind to {}:{}!", restHost, restPort))
  logger.info(s"Server online at http://$restHost:$restPort/")

  private def logRequestResult(level: LogLevel, route: Route) = {
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

  private def entityAsString(entity: HttpEntity): Future[String] = {
    entity.dataBytes
      .map(_.decodeString(entity.contentType.getCharsetOption.get().nioCharset()))
      .runWith(Sink.head)
  }

  /**
    * If instance has status "starting", "stopping" or "deleting"
    * instance will be stopped
    */
  private def stopInstances() = {
    logger.info("Running of crud-rest. Stop instances which have status \"starting\", \"stopping\" or \"deleting\".")
    val instances = instanceDAO.getAll.filter { instance =>
      instance.status.equals(starting) ||
        instance.status.equals(stopping) ||
        instance.status.equals(deleting)
    }.map(Instance.from)

    instances.foreach { instance =>
      new Thread(new InstanceStopper(instance, 1000)).start()
    }
  }
}
