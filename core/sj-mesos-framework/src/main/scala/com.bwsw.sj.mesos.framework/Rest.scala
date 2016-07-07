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

  def rest(port:Int): java.lang.Thread = new Thread(new Runnable {
    override def run(): Unit = {
      unfiltered.jetty.Server.http(port).plan(echo).run()
    }
  })

  def start(port:Int) = {
    rest(port).start()
  }

  def getResponse: String = {
    serializer.serialize(TasksList.toJson)
  }
}
