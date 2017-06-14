/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.output.benchmark

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.bwsw.common.JsonSerializer
import com.typesafe.config.ConfigFactory
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

  case class Entity(value: Int, stringValue: String, txn: Long) extends Serializable

  private val config = ConfigFactory.load()
  val httpPort = config.getInt(OutputBenchmarkConfigNames.restPort)
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
