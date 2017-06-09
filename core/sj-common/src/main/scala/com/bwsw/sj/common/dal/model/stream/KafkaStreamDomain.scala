package com.bwsw.sj.common.dal.model.stream

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import kafka.common.TopicAlreadyMarkedForDeletionException

import scala.util.{Failure, Success, Try}

class KafkaStreamDomain(override val name: String,
                        override val service: KafkaServiceDomain,
                        val partitions: Int,
                        val replicationFactor: Int,
                        override val description: String = RestLiterals.defaultDescription,
                        override val force: Boolean = false,
                        override val tags: Array[String] = Array(),
                        private val zkSessionTimeout: Int = ConfigLiterals.zkSessionTimeoutDefault)
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.kafkaStreamType) {

  protected def createClient(): KafkaClient = new KafkaClient(this.service.zkProvider.hosts)

  override def create(): Unit = {
    Try {
      val client = createClient()
      if (!client.topicExists(this.name)) {
        client.createTopic(this.name, this.partitions, this.replicationFactor)
      }

      client.close()
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot create a kafka topic ${this.name}. Topic is marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }
}