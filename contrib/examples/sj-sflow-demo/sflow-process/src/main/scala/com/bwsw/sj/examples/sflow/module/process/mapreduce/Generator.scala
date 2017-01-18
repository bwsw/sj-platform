package com.bwsw.sj.examples.sflow.module.process.mapreduce

import com.bwsw.sj.common.utils.SflowRecord
import com.hazelcast.config._
import com.hazelcast.core.{Hazelcast, HazelcastInstance, IMap}
import com.hazelcast.mapreduce.{JobTracker, KeyValueSource, Job}
import com.bwsw.sj.examples.sflow.module.process.mapreduce.mappers._

import scala.collection.JavaConverters._


class Generator {
  var hazelcastMapName = "hazelcast"
  var trackerName = "tracker"
  var hazelcastInstance: Option[HazelcastInstance] = None

  var tracker: Option[JobTracker] = None
  var source: Option[KeyValueSource[String, SflowRecord]] = None


  def SrcAsReduceResult(): collection.mutable.Map[Int, Int] = {
    val tracker = getTracker
    val source = getSource
    val job = tracker.newJob(source)
    val future = job.mapper(new SrcAsMapper()).reducer[Int](new CommonReducerFactory[Int]()).submit()
    future.get().asScala
  }

  def DstAsReduceResult(): collection.mutable.Map[Int, Int] = {
    val tracker = getTracker
    val source = getSource
    val job = tracker.newJob(source)
    val future = job.mapper(new DstAsMapper()).reducer[Int](new CommonReducerFactory[Int]).submit()
    future.get().asScala
  }

  def SrcDstReduceResult(): collection.mutable.Map[Int Tuple2 Int, Int] = {
    val tracker = getTracker
    val source = getSource
    val job = tracker.newJob(source)
    val future = job.mapper(new SrcDstAsMapper()).reducer[Int](new CommonReducerFactory[Int Tuple2 Int]).submit()
    future.get().asScala
  }

  def SrcIpReduceResult(): collection.mutable.Map[String, Int] = {
    val tracker = getTracker
    val source = getSource
    val job = tracker.newJob(source)
    val future = job.mapper(new SrcIpMapper()).reducer[Int](new CommonReducerFactory[String]).submit()
    future.get().asScala
  }

  def DstIpReduceResult(): collection.mutable.Map[String, Int] = {
    val tracker = getTracker
    val source = getSource
    val job = tracker.newJob(source)
    val future = job.mapper(new DstIpMapper()).reducer[Int](new CommonReducerFactory[String]).submit()
    future.get().asScala
  }

  def getSource: KeyValueSource[String, SflowRecord] = {
    if (source.isEmpty) source = Some(KeyValueSource.fromMap[String, SflowRecord] (getMap))
    source.get
  }

  def getTracker: JobTracker = {
    if (tracker.isEmpty) tracker = Some(getHazelcastInstance.getJobTracker(trackerName))
    tracker.get
  }

  def putRecords(records: Array[SflowRecord]) = {
    val imap: IMap[String, SflowRecord] = getHazelcastInstance.getMap[String, SflowRecord](hazelcastMapName)
    records.foreach(sflowRecord => imap.put(uuid, sflowRecord))
  }

  def getMap: IMap[String, SflowRecord] = {
    getHazelcastInstance.getMap[String, SflowRecord](hazelcastMapName)
  }

  def setHazelcastMapName(mapName: String) = hazelcastMapName = mapName


  def getHazelcastInstance: HazelcastInstance  = {
    if (hazelcastInstance.isEmpty) hazelcastInstance = Some(Hazelcast.newHazelcastInstance(getHazelcastConfig))
    hazelcastInstance.get
  }

  def getHazelcastConfig:Config = {
    val config = new XmlConfigBuilder().build

    val tcpIpConfig = new TcpIpConfig
    val hosts = Array("127.0.0.1").toList.asJava
    tcpIpConfig.setMembers(hosts).setEnabled(true)

    val joinConfig = new JoinConfig
    joinConfig.setMulticastConfig(new MulticastConfig().setEnabled(false))
    joinConfig.setTcpIpConfig(tcpIpConfig)

    val networkConfig = new NetworkConfig
    networkConfig.setJoin(joinConfig)
    config.setNetworkConfig(networkConfig)
  }

  def uuid = java.util.UUID.randomUUID.toString

  def clear() = getMap.destroy()
}



