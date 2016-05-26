package com.bwsw.sj.engine.output

import java.net.InetSocketAddress

import com.aerospike.client.Host
import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.{TStreamService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.tstreams.converter.ArrayByteToStringConverter
import com.bwsw.tstreams.data.aerospike.{AerospikeStorageOptions, AerospikeStorageFactory}
import com.bwsw.tstreams.metadata.MetadataStorageFactory

/**
  * Created: 5/26/16
  *
  * @author Kseniya Tomskikh
  */
object OutputDataFactory {

  val streamDAO: GenericMongoService[SjStream] = ConnectionRepository.getStreamService
  val streamName: String = "s1"
  val tStream: SjStream = streamDAO.get(streamName)
  val service: TStreamService = tStream.service.asInstanceOf[TStreamService]

  val aerospikeHosts = service.dataProvider.hosts.map { address =>
    val parts = address.split(":")
    new Host(parts(0), parts(1).toInt)
  }.toList

  val objectSerializer = new ObjectSerializer()

  val metadataStorageFactory = new MetadataStorageFactory
  val metadataStorage = metadataStorageFactory.getInstance(
    cassandraHosts = List(new InetSocketAddress("localhost", 9042)),
    keyspace = "test")

  val aerospikeStorageFactory = new AerospikeStorageFactory
  val aerospikeOptions = new AerospikeStorageOptions("test", aerospikeHosts)
  val aerospikeStorage = aerospikeStorageFactory.getInstance(aerospikeOptions)

  val arrayByteToStringConverter = new ArrayByteToStringConverter

}
