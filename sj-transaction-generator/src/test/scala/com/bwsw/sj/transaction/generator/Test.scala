package com.bwsw.sj.transaction.generator

import java.util.UUID

import com.bwsw.sj.transaction.generator.client.{TcpClientOptions, TcpClient}
import com.bwsw.sj.transaction.generator.server.TcpServer
import org.scalatest._

/**
  * Created: 4/18/16
  *
  * @author Kseniya Tomskikh
  */
class Test extends FlatSpec with Matchers {
  val options = new TcpClientOptions()
    .setZkServers(Array("127.0.0.1:2181"))
    .setPrefix("servers")
    .setRetryPeriod(500)
    .setRetryCount(10)
  val client = new TcpClient(options)
  val server = new TcpServer(null, null, "192.168.1.180", 8885)

  "TcpClient" should "connecting to server" in {
    client.open()
    client.socket.getPort shouldEqual 8885
    client.close()
  }

  "TcpServer" should "generating new transaction" in {
    server.getNewTransaction.isInstanceOf[UUID]
    server.getNewTransaction != null
  }

}
