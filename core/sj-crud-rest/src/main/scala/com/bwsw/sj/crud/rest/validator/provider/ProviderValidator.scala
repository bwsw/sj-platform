package com.bwsw.sj.crud.rest.validator.provider

import java.net.{InetAddress, InetSocketAddress, URI, URISyntaxException}
import java.nio.channels.ClosedChannelException
import java.util.Collections

import com.aerospike.client.{AerospikeException, AerospikeClient}
import com.bwsw.sj.common.DAL.model.Provider
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{ProviderConstants, ConfigSettingsUtils}
import com.bwsw.sj.common.rest.entities.provider.ProviderData
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
        errors += s"'Name' is required"
      case Some(x) =>
        if (providerDAO.get(x).isDefined) {
          errors += s"Provider with name '$x' already exists"
        }

        if (!validateName(x)) {
          errors += s"Provider has incorrect name: '$x'. " +
            s"Name of provider must be contain digits, lowercase letters or hyphens. First symbol must be a letter"
        }
    }

    // 'providerType field
    Option(initialData.providerType) match {
      case None =>
        errors += s"'Type' is required"
      case Some(x) =>
        if (!providerTypes.contains(x)) {
          errors += s"Unknown type '$x' provided. Must be one of: ${providerTypes.mkString("[", ", ", "]")}"
        }
    }

    //'hosts' field
    if (Option(initialData.hosts).isEmpty) {
      errors += s"'Hosts' is required"
    } else {
      if (initialData.hosts.isEmpty) {
        errors += s"'Hosts' must contain at least one host"
      } else {
        var ports = new ListBuffer[Int]()
        for (host <- initialData.hosts) {
          val (hostErrors, hostPort) = validateProviderHost(host, initialData.providerType)
          errors ++= hostErrors
          ports += hostPort
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
      val (hostErrors, port) = validateProviderHost(provider.providerType, host)
      errors ++= hostErrors
      if (errors.isEmpty) {
        checkProviderConnectionByType(
          provider.providerType,
          host,
          errors
        )
      }
    }

    errors
  }

  private def validateProviderHost(providerType: String, host: String): (ArrayBuffer[String], Int) = {
    val errors = ArrayBuffer[String]()
    var hostname: String = ""
    var port: Int = -1
    try {
      val uri = new URI(s"dummy://$host")
      hostname = uri.getHost
      port = uri.getPort

      if (hostname == null) {
        errors += s"Wrong host provided: '$host'"
      }

      val path = uri.getPath

      if (path.length > 0)
        errors += s"Host can not contain any uri path ('$path')"

    } catch {
      case ex: URISyntaxException =>
        errors += s"Wrong host provided: '$host'"
    }

    (errors, port)
  }

  private def checkProviderConnectionByType(providerType: String, host: String, errors: ArrayBuffer[String]) = {
    providerType match {
      case "cassandra" =>
        checkCassandraConnection(errors, host)
      case "aerospike" =>
        checkAerospikeConnection(errors, host)
      case "zookeeper" =>
        checkZookeeperConnection(errors, host)
      case "kafka" =>
        checkKafkaConnection(errors, host)
      case "ES" =>
        checkESConnection(errors, host)
      case "JDBC" =>
        checkJdbcConnection(errors, host)
      case _ =>
        errors += s"Host checking for provider type '$providerType' is not implemented"
    }
  }

  private def checkCassandraConnection(errors: ArrayBuffer[String], address: String) = {
    try {
      val (host, port) = getHostAndPort(address)

      val builder = Cluster.builder().addContactPointsWithPorts(new InetSocketAddress(host, port))

      val client = builder.build()
      client.getMetadata
      client.close()
    } catch {
      case ex: NoHostAvailableException =>
        errors += s"Cannot gain an access to Cassandra on '$address'"
      case _: Throwable =>
        errors += s"Wrong host '$address'"
    }
  }

  private def checkAerospikeConnection(errors: ArrayBuffer[String], address: String) = {
    val (host, port) = getHostAndPort(address)

    try {
      val client = new AerospikeClient(host, port)
      if (!client.isConnected) {
        errors += s"Cannot gain an access to Aerospike on '$address'"
      }
      client.close()
    } catch {
      case ex: AerospikeException =>
        errors += s"Cannot gain an access to Aerospike on '$address'"
    }
  }

  private def checkZookeeperConnection(errors: ArrayBuffer[String], address: String) = {
    var client: ZooKeeper = null
    try {
      client = new ZooKeeper(address, zkTimeout, null)
      val deadline = 1.seconds.fromNow
      var connected: Boolean = false
      while (!connected && deadline.hasTimeLeft) {
        connected = client.getState.isConnected
      }
      if (!connected) {
        errors += s"Can gain an access to Zookeeper on '$address'"
      }

    } catch {
      case ex: Throwable =>
        errors += s"Wrong host '$address'"
    }
    if (Option(client).isDefined) {
      client.close()
    }
  }

  private def checkKafkaConnection(errors: ArrayBuffer[String], address: String) = {
    val (host, port) = getHostAndPort(address)
    val consumer = new SimpleConsumer(host, port, 500, 64 * 1024, "connectionTest")
    val topics = Collections.singletonList("test_connection")
    val req = new TopicMetadataRequest(topics)
    try {
      consumer.send(req)
    } catch {
      case ex: ClosedChannelException =>
        errors += s"Can not establish connection to Kafka on '$address'"
      case ex: java.io.EOFException =>
        errors += s"Can not establish connection to Kafka on '$address'"
      case ex: Throwable =>
        errors += s"Some issues encountered while trying to establish connection to '$address'"
    }
  }

  private def checkESConnection(errors: ArrayBuffer[String], address: String) = {
    val (host, port) = getHostAndPort(address)
    val settings = Settings.settingsBuilder().put("client.transport.ping_timeout", "2s").build()
    val client = TransportClient.builder().settings(settings).build()
    client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port))
    if (client.connectedNodes().size() < 1) {
      errors += s"Can not establish connection to ElasticSearch on '$address'"
    }
  }

  private def checkJdbcConnection(errors: ArrayBuffer[String], address: String) = {
    true
  }

  private def getHostAndPort(address: String) = {
    val uri = new URI("dummy://" + address)
    val host = uri.getHost()
    val port = uri.getPort()

    (host, port)
  }
}
