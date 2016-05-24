package com.bwsw.sj.mesos.framework

import unfiltered.request._
import unfiltered.response._
import com.bwsw.common.JsonSerializer

/**
  * Created by diryavkin_dn on 16.05.16.
  */
object Rest {
  val serializer: JsonSerializer = new JsonSerializer()

  val echo = unfiltered.filter.Planify{
    case GET (Path("/")) => ResponseString(getResponse)
  }

  val rest: java.lang.Thread = new Thread(new Runnable {
    override def run(): Unit = {
      unfiltered.jetty.Server.http(8080).plan(echo).run()
    }
  })

  def start() = {
    rest.start()
  }

  def getResponse: String = {
    serializer.serialize(TasksList.toJson)
  }
}
