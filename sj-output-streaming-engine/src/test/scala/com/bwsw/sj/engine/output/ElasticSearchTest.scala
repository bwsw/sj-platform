package com.bwsw.sj.engine.output

import java.net.{InetSocketAddress, InetAddress}

import com.bwsw.common.JsonSerializer
import com.datastax.driver.core.utils.UUIDs
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

/**
  * Created: 6/24/16
  *
  * @author Kseniya Tomskikh
  */
object ElasticSearchTest {

  def main(args: Array[String]) = {
    println("start")

    val serializer = new JsonSerializer

    val client: TransportClient = TransportClient.builder().build()
    client.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress("localhost", 9300)))

    val indexRequestBuilder = client.prepareIndex("test", "estest", "2")

    val data = Map("txn" -> s"sfdfd${UUIDs.timeBased().toString.replaceAll("-", "A")}jkhfds", "message" -> "ss fsds ", "value" -> 120)
    val content = serializer.serialize(data)
    indexRequestBuilder.setSource(content)
    indexRequestBuilder .execute().actionGet()

    val getRequestBuilder = client.prepareGet("test", "estest", "2")
    getRequestBuilder.setFields("txn")
    val getresponse: GetResponse = getRequestBuilder.execute().actionGet()

    val value = getresponse.getField("txn").getValue.toString
    println(value)

    println("stop")
  }

}
