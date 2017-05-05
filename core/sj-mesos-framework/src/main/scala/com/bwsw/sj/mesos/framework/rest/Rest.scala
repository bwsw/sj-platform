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