package com.bwsw.sj.crud.rest.validator.provider

import java.net.{InetAddress, InetSocketAddress, URI, URISyntaxException}
import java.nio.channels.ClosedChannelException
import java.util.Collections

import com.aerospike.client.AerospikeClient
import com.bwsw.sj.common.DAL.model.Provider
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ProviderConstants, ConfigSettingsUtils}
import com.bwsw.sj.crud.rest.entities.provider.ProviderData
import com.bwsw.sj.crud.rest.utils.ValidationUtils
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import kafka.javaapi.TopicMetadataRequest
import kafka.javaapi.consumer.SimpleConsumer
import org.apache.zookeeper.ZooKeeper
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.slf4j.LoggerFactory

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.duration._

object ProviderValidator extends ValidationUtils {

  import ProviderConstants._

  private val logger = LoggerFactory.getLogger(getClass.getName)
  private val zkTimeout = ConfigSettingsUtils.getZkSessionTimeout()

  /**
   * Validating input parameters for provider
   *
   * @param initialData - input parameters for service being validated
   * @return - errors
   */
  def validate(initialData: ProviderData) = {
    logger.debug(s"Provider ${initialData.name}. Start provider validation.")

    val errors = new ArrayBuffer[String]()

    val providerDAO = ConnectionRepository.getProviderService

    // 'name' field
    Option(initialData.name) match {
      case None =>
        errors += s"'name' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'name' can not be empty"
        } else {
          if (providerDAO.get(x).isDefined) {
            errors += s"Provider with name $x already exists"
          }
        }
    }

    if (!validateName(initialData.name)) {
      errors += s"Provider has incorrect name: ${initialData.name}. Name of provider must be contain digits, letters or hyphens. First symbol must be letter."
    }

    // 'description' field
    Option(initialData.description) match {
      case None =>
        errors += s"'description' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'description' can not be empty"
        }
    }

    // 'login' field
    Option(initialData.login) match {
      case None =>
        errors += s"'login' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'login' can not be empty"
        }
    }

    // 'password' field
    Option(initialData.description) match {
      case None =>
        errors += s"'description' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'description' can not be empty"
        }
    }

    // 'providerType field
    Option(initialData.providerType) match {
      case None =>
        errors += s"'type' is required"
      case Some(x) =>
        if (!providerTypes.contains(x)) {
          errors += s"Unknown 'type' provided. Must be one of: ${providerTypes.mkString("[", "|", "]")}"
        }
    }

    //'hosts' field
    if (Option(initialData.hosts).isEmpty) {
      errors += s"'hosts' is required"
    } else {
      if (initialData.hosts.isEmpty) {
        errors += s"'hosts' must contain at least one host"
      } else {
        var ports = new ListBuffer[Int]()
        for (host <- initialData.hosts) {
          val (hostErrors, hostPort) = validateProviderConnection(host, initialData.providerType)
          errors ++= hostErrors
          ports += hostPort
        }

        if (initialData.providerType == "cassandra" && ports.distinct.size > 1) {
          errors += s"Ports must be the same for all hosts of '${initialData.providerType}' provider subset"
        }
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
      val (hostErrors, port) = validateProviderConnection(provider.providerType, host)
      errors ++= hostErrors
      if (errors.isEmpty) {
        checkProviderConnectionByType(
          provider.providerType,
          host,
          port,
          errors
        )
      }
    }

    errors
  }

  private def validateProviderConnection(providerType: String, host: String): (ArrayBuffer[String], Int) = {
    val errors = ArrayBuffer[String]()
    var hostname: String = ""
    var port: Int = -1
    try {
      val uri = new URI(s"dummy://$host")
      hostname = uri.getHost
      port = uri.getPort
      val path = uri.getPath

      if (path.length > 0)
        errors += s"host can not contain any uri path ('$path')"

    } catch {
      case ex: URISyntaxException =>
        errors += s"Wrong host provided: '$host'"
    }

    (errors, port)
  }

  private def checkProviderConnectionByType(providerType: String, host: String, port: Int, errors: ArrayBuffer[String]) = {
    providerType match {
      case "cassandra" =>
        checkCassandraConnection(errors, host, port)
      case "aerospike" =>
        checkAerospikeConnection(errors, host, port)
      case "zookeeper" =>
        checkZookeeperConnection(errors, host, port)
      case "kafka" =>
        checkKafkaConnection(errors, host, port)
      case "ES" =>
        checkESConnection(errors, host, port)
      case "redis" =>
      // TODO: remove redis in future. So returning no errors for now.
      case "JDBC" =>
        checkJdbcConnection(errors, host, port)
      case _ =>
        errors += s"host checking for provider type '$providerType' is not implemented"
    }
  }

  private def checkCassandraConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    try {
      val builder =
        if (port == -1)
          Cluster.builder().addContactPoint(hostname)
        else
          Cluster.builder().addContactPointsWithPorts(new InetSocketAddress(hostname, port))

      val client = builder.build()
      val metadata = client.getMetadata
      client.close()
    } catch {
      case ex: NoHostAvailableException =>
        errors += s"Cannot access Cassandra on '$hostname'"
      case _: Throwable =>
        errors += s"Wrong host '$hostname'"
    }
  }

  private def checkAerospikeConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val aerospikePort = if (port == -1) 3000 else port
    val client = new AerospikeClient(hostname, aerospikePort)
    if (!client.isConnected) {
      errors += s"Cannot access Aerospike on '$hostname:$port'"
    }
    client.close()
  }

  private def checkZookeeperConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val zookeeperPort = if (port == -1) 2181 else port
    var client: ZooKeeper = null
    try {
      client = new ZooKeeper(s"$hostname:$zookeeperPort", zkTimeout, null)
      val deadline = 1.seconds.fromNow
      var connected: Boolean = false
      while (!connected && deadline.hasTimeLeft) {
        connected = client.getState.isConnected
      }
      if (!connected) {
        errors += s"Can not access Zookeeper on '$hostname:$zookeeperPort'"
      }

    } catch {
      case ex: Throwable =>
        errors += s"Wrong zookeeper host"
    }
    if (Option(client).isDefined) {
      client.close()
    }
  }

  private def checkKafkaConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val kafkaPort = if (port == -1) 9092 else port
    val consumer = new SimpleConsumer(hostname, kafkaPort, 500, 64 * 1024, "connectionTest")
    val topics = Collections.singletonList("test_connection")
    val req = new TopicMetadataRequest(topics)
    try {
      consumer.send(req)
    } catch {
      case ex: ClosedChannelException =>
        errors += s"'$hostname:$kafkaPort' does not respond"
      case ex: java.io.EOFException =>
        errors += s"Can not establish connection to Kafka on '$hostname:$kafkaPort'"
      case ex: Throwable =>
        errors += s"Some issues encountered while trying to establish connection to '$hostname:$kafkaPort'"
    }
  }

  private def checkESConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    val esPort = if (port == -1) 9300 else port
    val settings = Settings.settingsBuilder().put("client.transport.ping_timeout", "2s").build()
    val client = TransportClient.builder().settings(settings).build()
    client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostname), esPort))
    if (client.connectedNodes().size() < 1) {
      errors += s"Can not establish connection to ElasticSearch on '$hostname:$esPort'"
    }
  }

  private def checkJdbcConnection(errors: ArrayBuffer[String], hostname: String, port: Int) = {
    true
  }

}
