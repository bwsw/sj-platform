package com.bwsw.sj.common.utils

import com.bwsw.sj.common.DAL.model.TStreamService
import com.bwsw.tstreams.data.IStorage

private[common] object SjStreamUtilsForCreation {

  def createMetadataStorage(service: TStreamService) = {
    val metadataProvider = service.metadataProvider
    val hosts = splitHosts(metadataProvider.hosts)
    val cassandraFactory = new CassandraFactory()
    cassandraFactory.open(hosts)
    val metadataStorage = cassandraFactory.getMetadataStorage(service.metadataNamespace)
    cassandraFactory.close()

    metadataStorage
  }

  def createDataStorage(service: TStreamService) = {
    val dataProvider = service.dataProvider
    val hosts = splitHosts(dataProvider.hosts)
    var dataStorage: IStorage[Array[Byte]] = null
    dataProvider.providerType match {
      case ProviderLiterals.cassandraType =>
        val cassandraFactory = new CassandraFactory()
        cassandraFactory.open(hosts)
        dataStorage = cassandraFactory.getDataStorage(service.dataNamespace)
        cassandraFactory.close()
      case ProviderLiterals.aerospikeType =>
        val aerospikeFactory = new AerospikeFactory
        dataStorage = aerospikeFactory.getDataStorage(service.dataNamespace, hosts)
        aerospikeFactory.close()
    }

    dataStorage
  }

  private def splitHosts(hosts: Array[String]) = {
    hosts.map(s => {
      val address = s.split(":")
      (address(0), address(1).toInt)
    }).toSet
  }
}
