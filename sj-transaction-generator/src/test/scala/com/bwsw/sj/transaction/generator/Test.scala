package com.bwsw.sj.transaction.generator

import java.util.UUID

import com.bwsw.sj.transaction.generator.client.TcpClient
import com.bwsw.sj.transaction.generator.server.TcpServer
import org.scalatest._

/**
  * Created: 4/18/16
  *
  * @author Kseniya Tomskikh
  */
class Test extends FlatSpec with Matchers {
  val client = new TcpClient("192.168.1.180", 8885)
  val server = new TcpServer(8885)

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
