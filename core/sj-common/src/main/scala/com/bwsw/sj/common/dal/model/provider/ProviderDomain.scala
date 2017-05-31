package com.bwsw.sj.common.dal.model.provider

import java.net.{InetSocketAddress, URI}
import java.nio.channels.ClosedChannelException
import java.util.Collections
import java.util.concurrent.TimeUnit

import com.aerospike.client.{AerospikeClient, AerospikeException}
import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import kafka.javaapi.TopicMetadataRequest
import kafka.javaapi.consumer.SimpleConsumer
import org.apache.zookeeper.ZooKeeper
import org.eclipse.jetty.client.HttpClient
import org.mongodb.morphia.annotations.Entity
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

@Entity("providers")
class ProviderDomain(@IdField val name: String,
                     val description: String,
                     val hosts: Array[String],
                     val login: String,
                     val password: String,
                     @PropertyField("provider-type") val providerType: String) {

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: ProviderDomain => that.name.equals(this.name) && that.providerType.equals(this.providerType)
    case _ => false
  }

  def getConcatenatedHosts(separator: String = ","): String = {
    hosts.mkString(separator)
  }

  def checkConnection()(implicit injector: Injector): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    for (host <- this.hosts) {
      errors ++= checkProviderConnectionByType(host, this.providerType)
    }

    errors
  }

  private def checkProviderConnectionByType(host: String, providerType: String)
                                           (implicit injector: Injector): ArrayBuffer[String] = {
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

  private def checkCassandraConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    Try {
      val (host, port) = getHostAndPort(address)

      val builder = Cluster.builder().addContactPointsWithPorts(new InetSocketAddress(host, port))

      val client = builder.build()
      client.getMetadata
      client.close()
    } match {
      case Success(_) =>
      case Failure(_: NoHostAvailableException) =>
        errors += s"Cannot gain an access to Cassandra on '$address'"
      case Failure(_) =>
        errors += s"Wrong host '$address'"
    }

    errors
  }

  private def checkAerospikeConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)

    Try(new AerospikeClient(host, port)) match {
      case Success(client) =>
        if (!client.isConnected) {
          errors += s"Cannot gain an access to Aerospike on '$address'"
        }
        client.close()
      case Failure(_: AerospikeException) =>
        errors += s"Cannot gain an access to Aerospike on '$address'"
      case Failure(e) => throw e
    }

    errors
  }

  private def checkZookeeperConnection(address: String)
                                      (implicit injector: Injector): ArrayBuffer[String] = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

    val errors = ArrayBuffer[String]()
    val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
    connectionRepository.getConfigRepository.get(ConfigLiterals.zkSessionTimeoutTag) match {
      case Some(config) =>
        val zkTimeout = config.value.toInt
        Try(new ZooKeeper(address, zkTimeout, null)) match {
          case Success(client) =>
            val deadline = 1.seconds.fromNow
            var connected: Boolean = false
            while (!connected && deadline.hasTimeLeft) {
              connected = client.getState.isConnected
            }
            if (!connected) {
              errors += s"Can gain an access to Zookeeper on '$address'"
            }
            client.close()
          case Failure(_) =>
            errors += s"Wrong host '$address'"
        }
      case None => errors += createMessage("entity.error.config.required", ConfigLiterals.zkSessionTimeoutTag)
    }

    errors
  }

  private def checkKafkaConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)
    val consumer = new SimpleConsumer(host, port, 500, 64 * 1024, "connectionTest")
    val topics = Collections.singletonList("test_connection")
    val req = new TopicMetadataRequest(topics)
    Try(consumer.send(req)) match {
      case Success(_) =>
      case Failure(_: ClosedChannelException) | Failure(_: java.io.EOFException) =>
        errors += s"Can not establish connection to Kafka on '$address'"
      case Failure(_) =>
        errors += s"Some issues encountered while trying to establish connection to '$address'"
    }

    errors
  }

  private def checkESConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val client = new ElasticsearchClient(Set(getHostAndPort(address)))
    if (!client.isConnected()) {
      errors += s"Can not establish connection to ElasticSearch on '$address'"
    }
    client.close()

    errors
  }

  protected def checkJdbcConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  private def checkHttpConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val uri = new URI("http://" + address)
    val client = new HttpClient()
    val timeout = 10
    client.start()
    Try {
      client
        .newRequest(uri)
        .timeout(timeout, TimeUnit.SECONDS)
        .send()
    } match {
      case Success(_) =>
      case Failure(_) =>
        errors += s"Can not establish connection to REST on '$address'"
    }
    client.stop()

    errors
  }

  private def getHostAndPort(address: String): (String, Int) = {
    val uri = new URI("dummy://" + address)
    val host = uri.getHost()
    val port = uri.getPort()

    (host, port)
  }
}
