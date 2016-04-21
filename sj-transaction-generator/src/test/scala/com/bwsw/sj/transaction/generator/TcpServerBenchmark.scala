package com.bwsw.sj.transaction.generator


import java.util.concurrent.TimeUnit
import java.util.Calendar

import com.bwsw.sj.transaction.generator.client.TcpClient

/**
  * Created: 18/04/2016
  *
  * @author Kseniya Tomskikh
  */
object TcpServerBenchmark {

  def main(args: Array[String]) = {
    val client = new TcpClient(null)
    client.open()
    var i = 0
    println(s"Start time: ${Calendar.getInstance().getTime.toString}")
    val time = TimeUnit.MINUTES.toNanos(1)
    val stop = System.nanoTime() + time
    while (System.nanoTime() < stop) {
      client.get()
      i += 1
    }
    println(s"End time: ${Calendar.getInstance().getTime.toString}")
    println(s"$i req")
    println(s"${i/TimeUnit.NANOSECONDS.toSeconds(time)} req/s")
  }

}
