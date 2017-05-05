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
import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.utils.EngineLiterals
import EngineLiterals._
import com.bwsw.sj.crud.rest.instance.InstanceStopper

import scala.concurrent.Future

/**
 * Run object of CRUD Rest-API
 *
 *
 * @author Kseniya Tomskikh
 */
object SjCrudRestService extends App with SjCrudInterface {
  implicit val system = ActorSystem("sj-crud-rest-server")
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.dispatcher

  require(Option(System.getenv("CRUD_REST_HOST")).isDefined && Option(System.getenv("CRUD_REST_PORT")).isDefined,
  "No environment variables: CRUD_REST_HOST, CRUD_REST_PORT")

  val restHost = System.getenv("CRUD_REST_HOST")
  val restPort = System.getenv("CRUD_REST_PORT").toInt
  val serializer = new JsonSerializer()
  serializer.setIgnoreUnknown(true)
  val storage = ConnectionRepository.getFileStorage
  val fileMetadataDAO = ConnectionRepository.getFileMetadataService
  val instanceDAO = ConnectionRepository.getInstanceService
  val serviceDAO = ConnectionRepository.getServiceManager
  val providerDAO = ConnectionRepository.getProviderService
  val streamDAO = ConnectionRepository.getStreamService
  val configService = ConnectionRepository.getConfigService
  val routeLogged = logRequestResult(Logging.InfoLevel, route())
  val logger = Logging(system, getClass)

  //putRestSettingsToConfigFile()

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

  private def putRestSettingsToConfigFile() = {
    configService.save(new ConfigurationSetting(ConfigLiterals.hostOfCrudRestTag, restHost, ConfigLiterals.systemDomain))
    configService.save(new ConfigurationSetting(ConfigLiterals.portOfCrudRestTag, restPort.toString, ConfigLiterals.systemDomain))
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
    }

    instances.foreach { instance =>
      new Thread(new InstanceStopper(instance, 1000)).start()
    }
  }
}
