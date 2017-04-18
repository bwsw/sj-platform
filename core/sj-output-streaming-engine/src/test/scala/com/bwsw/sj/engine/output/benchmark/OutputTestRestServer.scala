package com.bwsw.sj.engine.output.benchmark

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.bwsw.common.JsonSerializer
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Request, Server}

import scala.collection.mutable.ListBuffer

/**
  * HTTP server for RESTful-output benchmark.
  * Environment: HTTP_PORT=44500
  *
  * @author Pavel Tomskikh
  */
object OutputTestRestServer extends App {

  case class Entity(value: Integer, stringValue: String, txn: Long) extends Serializable

  val httpPort = System.getenv("HTTP_PORT").toInt
  val jsonSerializer = new JsonSerializer
  val storage = new ListBuffer[Entity]()

  val handler = new AbstractHandler {
    override def handle(
        path: String,
        request: Request,
        httpServletRequest: HttpServletRequest,
        response: HttpServletResponse) = {
      request.getMethod match {
        case "GET" =>
          println("GET")
          response.setStatus(HttpServletResponse.SC_OK)
          response.setContentType("application/json;charset=utf-8")
          val writer = response.getWriter
          val data = jsonSerializer.serialize(storage.toList)
          data.foreach(writer.println)
        case "POST" =>
          println("POST")
          val reader = request.getReader
          val data = reader.lines().toArray.map(_.asInstanceOf[String]).mkString
          val entity = jsonSerializer.deserialize[Entity](data)
          println(s"  $entity")
          storage += entity
        case "DELETE" =>
          println("DELETE")
          val txn = request.getParameter("txn").toLong
          println(s"  txn=$txn")
          storage.find(_.txn == txn) match {
            case Some(e) =>
              storage -= e
              response.setStatus(HttpServletResponse.SC_OK)
            case None =>
              response.setStatus(HttpServletResponse.SC_NOT_FOUND)
          }
        case _ =>
          println("UNKNOWN")
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
      }
      request.setHandled(true)
    }
  }

  val server = new Server(httpPort)
  server.setHandler(handler)
  server.start()
  server.join()
}
