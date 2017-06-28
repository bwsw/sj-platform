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
package com.bwsw.sj.mesos.framework.rest

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.mesos.framework.task.TasksList

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Server, Request}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

private class Handler extends AbstractHandler {
  val serializer: JsonSerializer = new JsonSerializer()

  override def handle(target: String,
                      req: Request,
                      httpReq: HttpServletRequest,
                      httpRes: HttpServletResponse): Unit = {
    httpRes.setContentType("application/json")
    httpRes.setStatus(HttpServletResponse.SC_OK)
    httpRes.getWriter().println(serializer.serialize(TasksList.toFrameworkTask))
    req.setHandled(true)
  }
}

/**
  * Rest object used for show some information about framework tasks.
  */
object Rest {

  def start(port: Int): Server = {
    val server = new Server(port)
    server.setHandler(new Handler)
    server.start

    server
  }
}