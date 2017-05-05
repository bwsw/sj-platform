package com.bwsw.sj.common._dal.model.provider

import java.net.{InetSocketAddress, URI, URISyntaxException}
import java.nio.channels.ClosedChannelException
import java.util.Collections
import java.util.concurrent.TimeUnit

import com.aerospike.client.{AerospikeClient, AerospikeException}
import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common._dal.morphia.MorphiaAnnotations.{IdField, PropertyField}
import com.bwsw.sj.common._dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import com.bwsw.sj.common.utils.ProviderLiterals.types
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import kafka.javaapi.TopicMetadataRequest
import kafka.javaapi.consumer.SimpleConsumer
import org.apache.zookeeper.ZooKeeper
import org.eclipse.jetty.client.HttpClient
import org.mongodb.morphia.annotations.Entity

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import MessageResourceUtils._
import ValidationUtils._

@Entity("providers")
class Provider(@IdField val name: String,
               val description: String,
               val hosts: Array[String],
               val login: String,
               val password: String,
               @PropertyField("provider-type") val providerType: String) {

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: Provider => that.name.equals(this.name) && that.providerType.equals(this.providerType)
    case _ => false
  }

  def getHosts() = {
    val inetSocketAddresses = this.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet

    inetSocketAddresses
  }

  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Provider", x, "provider")
          }

          if (providerDAO.get(x).isDefined) {
            errors += createMessage("entity.error.already.exists", "Provider", x)
          }
        }
    }

    // 'providerType field
    Option(this.providerType) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Type")
        }
        else {
          if (!types.contains(x)) {
            errors += createMessage("entity.error.unknown.type.must.one.of", x, "provider", types.mkString("[", ", ", "]"))
          }
        }
    }

    //'hosts' field
    Option(this.hosts) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Hosts")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.hosts.should.be.non.empty")
        } else {
          if (x.head.isEmpty) {
            errors += createMessage("entity.error.attribute.required", "Hosts")
          }
          else {
            for (host <- this.hosts) {
              errors ++= validateHost(host)
            }
          }
        }
    }

    errors
  }

  private def validateHost(host: String): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    try {
      val uri = new URI(s"dummy://${normalizeName(host)}")
      val hostname = uri.getHost

      if (hostname == null) {
        errors += createMessage("entity.error.wrong.host", host)
      }

      if (uri.getPort == -1) {
        errors += createMessage("entity.error.host.must.contains.port", host)
      }

      val path = uri.getPath

      if (path.length > 0)
        errors += createMessage("entity.error.host.should.not.contain.uri", path)

    } catch {
      case ex: URISyntaxException =>
        errors += createMessage("entity.error.wrong.host", host)
    }

    errors
  }

  def checkConnection() = {
    val errors = ArrayBuffer[String]()
    for (host <- this.hosts) {
      errors ++= checkProviderConnectionByType(host, this.providerType)
    }

    errors
  }

  private def checkProviderConnectionByType(host: String, providerType: String) = {
    providerType match {
      case ProviderLiterals.cassandraType =>
        checkCassandraConnection(host)
      case ProviderLiterals.aerospikeType =>
        checkAerospikeConnection(host)
      case ProviderLiterals.zookeeperType =>
        checkZookeeperConnection(host)
      case ProviderLiterals.kafkaType =>
        checkKafkaConnection(host)
      case ProviderLiterals.elasticsearchType =>
        checkESConnection(host)
      case ProviderLiterals.jdbcType =>
        checkJdbcConnection(host)
      case ProviderLiterals.restType =>
        checkHttpConnection(host)
      case _ =>
        throw new Exception(s"Host checking for provider type '$providerType' is not implemented")
    }
  }

  private def checkCassandraConnection(address: String) = {
    val errors = ArrayBuffer[String]()
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

    errors
  }

  private def checkAerospikeConnection(address: String) = {
    val errors = ArrayBuffer[String]()
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

    errors
  }

  private def checkZookeeperConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
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

    errors
  }

  private def checkKafkaConnection(address: String) = {
    val errors = ArrayBuffer[String]()
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

    errors
  }

  private def checkESConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    val client = new ElasticsearchClient(Set(getHostAndPort(address)))
    if (!client.isConnected()) {
      errors += s"Can not establish connection to ElasticSearch on '$address'"
    }
    client.close()

    errors
  }

  protected def checkJdbcConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  private def checkHttpConnection(address: String) = {
    val errors = ArrayBuffer[String]()
    val uri = new URI("http://" + address)
    val client = new HttpClient()
    val timeout = 10
    client.start()
    try {
      client
        .newRequest(uri)
        .timeout(timeout, TimeUnit.SECONDS)
        .send()
    } catch {
      case _: Throwable =>
        errors += s"Can not establish connection to REST on '$address'"
    }
    client.stop()

    errors
  }

  private def getHostAndPort(address: String) = {
    val uri = new URI("dummy://" + address)
    val host = uri.getHost()
    val port = uri.getPort()

    (host, port)
  }
}
