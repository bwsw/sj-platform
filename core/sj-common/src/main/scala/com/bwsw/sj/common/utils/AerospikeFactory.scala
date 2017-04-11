//package com.bwsw.sj.common.utils
//
//import com.aerospike.client.Host
//import com.bwsw.tstreams.data.aerospike
//
//class AerospikeFactory() {
//  private val dataStorageFactory = new aerospike.Factory()
//
//  def getDataStorage(namespace: String, hosts: Set[(String, Int)]) = {
//    val dataHosts = hosts.map(s => new Host(s._1, s._2))
//    val options = new aerospike.Options(
//      namespace,
//      dataHosts
//    )
//
//    dataStorageFactory.getInstance(options)
//  }
//
//  def close() = {
//    dataStorageFactory.closeFactory()
//  }
//}
//todo after integration with t-streams