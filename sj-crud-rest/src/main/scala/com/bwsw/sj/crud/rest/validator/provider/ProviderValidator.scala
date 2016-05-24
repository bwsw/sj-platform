package com.bwsw.sj.crud.rest.validator.provider

import java.net.{InetAddress, InetSocketAddress, URI, URISyntaxException}
import java.nio.channels.ClosedChannelException
import java.util.Collections

import com.aerospike.client.AerospikeClient
import com.bwsw.sj.common.DAL.model.Provider
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.crud.rest.entities._
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import kafka.javaapi.consumer.SimpleConsumer
import kafka.javaapi.{TopicMetadataRequest, TopicMetadataResponse}
import org.apache.zookeeper.ZooKeeper
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.duration._

/**
  * Created by mendelbaum_nm on 13.05.16.
  */
class ProviderValidator {
  import com.bwsw.sj.common.ProviderConstants._

  var providerDAO: GenericMongoService[Provider] = null

  /**
    * Validating input parameters for provider
    *
    * @param initialData - input parameters for service being validated
    * @return - errors
    */
  def validate(initialData: ProviderData) = {

    val errors = new ArrayBuffer[String]()

    providerDAO = ConnectionRepository.getProviderService

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'name' can not be empty"
        } else {
          if (providerDAO.get(x) != null) {
            errors += s"Provider with name $x already exists"
          }
        }
    }

    // 'description' field
    Option(initialData.description) match {
      case None =>
        errors += s"'description' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'description' can not be empty"
    }

    // 'login' field
    Option(initialData.login) match {
      case None =>
        errors += s"'login' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'login' can not be empty"
    }

    // 'password' field
    Option(initialData.description) match {
      case None =>
        errors += s"'description' is required"
      case Some(x) =>
        if (x.isEmpty)
          errors += s"'description' can not be empty"
    }

    // 'providerType field
    Option(initialData.providerType) match {
      case None =>
        errors += s"'type' is required"
      case Some(x) =>
        if (!providerTypes.contains(x))
          errors += s"Unknown 'type' provided. Must be one of: ${providerTypes.mkString("[","|","]")}"
    }

    //'hosts' field
    if (Option(initialData.hosts).isEmpty) {
      errors += s"'hosts' is required"
    }
    else {
      if (initialData.hosts.isEmpty) {
        errors += s"'hosts' must contain at least one host"
      }
      else {
        var ports = new ListBuffer[Int]()
        for (host <- initialData.hosts) {
          val (hostErrors, hostPort) = validateHost(host, initialData.providerType)
          errors ++= hostErrors
          ports += hostPort
        }

        if (initialData.providerType == "cassandra" && ports.distinct.size > 1)
          errors += s"Ports must be the same for all hosts of '${initialData.providerType}' provider subset"
      }
    }

    errors
  }

  /**
    * Check provider hosts availability
    *
    * @param provider - provider entity
    * @return - errors array
    */
  def checkProviderConnection(provider: Provider) = {
    var errors = ArrayBuffer[String]()
    for (host <- provider.hosts) {
      val (hostErrors, _) = validateHost(host, provider.providerType)
      errors ++= hostErrors
    }
    errors
  }

  def validateHost (hostString: String, providerType: String): (ArrayBuffer[String], Int) = {
    val errors = ArrayBuffer[String]()
    var hostname : String = null
    var port : Int = -1
    try {
      val uri = new URI(s"dummy://$hostString")
      hostname = uri.getHost
      port = uri.getPort
      val path = uri.getPath

      if (path.length > 0)
        errors += s"host can not contain any uri path ('$path')"

    } catch {
      case ex: URISyntaxException =>
        errors += s"Wrong host provided: '$hostString'"
    }

    if (errors.isEmpty) {
      providerType match {
        case "cassandra" =>
          checkCassandraConnection(errors, hostname, port)
        case "aerospike" =>
          checkAerospikeConnection(errors, hostname, port)
        case "zookeeper" =>
          checkZookeeperConnection(errors, hostname, port)
        case "kafka" =>
          checkKafkaConnection(errors, hostname, port)
        case "ES" =>
          checkESConnection(errors, hostname, port)
        case "redis" =>
          // TODO: remove redis in future. So returning no errors for now.
        case "JDBC" =>
          checkJdbcConnection(errors, hostname, port)
        case _ =>
          errors += s"host checking for provider type '$providerType' is not implemented"
      }
    }
    (errors, port)
  }

  def checkCassandraConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    try {
      val builder =
        if (port == -1)
          Cluster.builder().addContactPoint(hostname)
        else
          Cluster.builder().addContactPointsWithPorts(new InetSocketAddress(hostname, port))

      val client = builder.build()
      val metadata = client.getMetadata
      client.close()
    }
    catch {
      case ex: NoHostAvailableException =>
        errors += s"Cannot access Cassandra on '$hostname'"
      case _: Throwable =>
        errors += s"Wrong host '$hostname'"
    }
  }

  def checkAerospikeConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val aerospikePort = if (port == -1) 3000 else port
    val client = new AerospikeClient(hostname, aerospikePort)
    if (!client.isConnected)
      errors += s"Cannot access Aerospike on '$hostname:$port'"
    client.close()
  }

  def checkZookeeperConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val zookeeperPort = if (port == -1) 2181 else port
    var client: ZooKeeper = null
    try {
      client = new ZooKeeper(s"$hostname:$zookeeperPort", 500, null)
      val deadline = 1.seconds.fromNow
      var connected: Boolean = false
      while (!connected && deadline.hasTimeLeft) {
        connected = client.getState.isConnected
      }
      if (!connected)
        errors += s"Can not access Zookeeper on '$hostname:$zookeeperPort'"

    } catch {
      case ex: Throwable =>
        errors += s"Wrong zookeeper host"
    }
    if (Option(client).isDefined)
      client.close()
  }

  def checkKafkaConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val kafkaPort = if (port == -1) 9092 else port
    val consumer = new SimpleConsumer(hostname, kafkaPort, 500, 64 * 1024, "connectionTest")
    val topics = Collections.singletonList("test_connection")
    val req = new TopicMetadataRequest(topics)
    var resp: TopicMetadataResponse = null
    try {
      resp = consumer.send(req)
    }
    catch {
      case ex: ClosedChannelException =>
        errors += s"'$hostname:$kafkaPort' does not respond"
      case ex: java.io.EOFException =>
        errors += s"Can not establish connection to Kafka on '$hostname:$kafkaPort'"
      case ex: Throwable =>
        errors += s"Some issues encountered while trying to establish connection to '$hostname:$kafkaPort'"
    }
  }

  def checkESConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val esPort = if (port == -1) 9300 else port
    val client = TransportClient.builder().build()
    client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostname), esPort))
    if (client.connectedNodes().size() < 1)
      errors += s"Can not establish connection to ElasticSearch on '$hostname:$esPort'"
  }

  def checkJdbcConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {

  }

}
