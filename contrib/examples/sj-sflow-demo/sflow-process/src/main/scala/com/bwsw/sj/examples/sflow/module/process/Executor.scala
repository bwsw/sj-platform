package com.bwsw.sj.examples.sflow.module.process

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.utils.{GeoIp, SflowRecord}
import com.bwsw.sj.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.windowed.{WindowRepository, WindowedStreamingExecutor}
import com.hazelcast.core.{Hazelcast, HazelcastInstance}

import java.util.concurrent.atomic.AtomicInteger
import com.hazelcast.mapreduce._
import com.hazelcast.config._
import scala.collection.JavaConverters._

import com.hazelcast.config.{Config, TcpIpConfig, XmlConfigBuilder}

import scala.collection.mutable.ArrayBuffer


class AsMapper extends Mapper[String, SflowRecord, Int, Int] {
  override def map(key: String, value: SflowRecord, context: Context[Int, Int]) = {
    context.emit(value.dstAs, value.packetSize * value.samplingRate)
  }
}

class AsReducerFactory extends ReducerFactory[Int, Int, Int] {
  override def newReducer(key: Int) = {
    new AsReducer()
  }
}

class AsReducer extends Reducer[Int, Int] {
  val sum: AtomicInteger = new AtomicInteger(0)

  override def reduce(value: Int) = {
    sum.addAndGet(value)
  }

  override def finalizeReduce(): Int = {
    sum.get()
  }
}


class Executor(manager: ModuleEnvironmentManager) extends WindowedStreamingExecutor(manager) {
  private val objectSerializer = new ObjectSerializer()
  private val storage = ArrayBuffer[SflowRecord]()
  private val hazelcastMapName = "hzname"

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val allWindows = windowRepository.getAll()

    val envelopes = allWindows.flatMap(_._2.batches).flatMap(_.envelopes).map(_.asInstanceOf[TStreamEnvelope])
    val sflowRecords = envelopes.flatMap(_.data.map(bytes => {
      val sflowRecord = objectSerializer.deserialize(bytes).asInstanceOf[SflowRecord]
      sflowRecord.srcAs = GeoIp.resolveAs(sflowRecord.srcIP)
      sflowRecord.dstAs = GeoIp.resolveAs(sflowRecord.dstIP)

      sflowRecord
    }))

    storage ++= sflowRecords

//    storage.map(x => x.dstAs -> x.packetSize * x.samplingRate).reduce(_ + _)
  }

  override def onEnter(): Unit = {
    val hz1 = getHazelcastInstance

    val imap = hz1.getMap[String, SflowRecord](hazelcastMapName)

    storage.foreach(sflowRecord =>
      imap.put(uuid, sflowRecord)
    )


    val tracker = hz1.getJobTracker("tracker")
    val source = KeyValueSource.fromMap[String, SflowRecord] (imap)
    val job = tracker.newJob(source)
    val future = job.mapper(new AsMapper()).reducer(new AsReducerFactory()).submit()
    val result = future.get()
  }

  def getHazelcastInstance: HazelcastInstance  = {
    Hazelcast.newHazelcastInstance(getHazelcastConfig)
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
}



object ap extends App {
  val hazelcastMapName = "hz"


  def getHazelcastInstance: HazelcastInstance  = {
    Hazelcast.newHazelcastInstance(getHazelcastConfig)
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

  val params = Array(
    Array("1477539632","FLOW","10.252.0.1","65","65","064222000125","001ef7f64900","0x0800","31","171","176.120.25.59","78.140.62.109","6","0x00","63","8090","55232","0x18","1328","1306","512"),
    Array("1477539632","FLOW","10.252.0.1","65","65","064222000125","001ef7f64900","0x0800","31","171","176.120.25.59","78.140.62.109","6","0x00","63","8090","55232","0x18","1328","1306","512")
  )
  val storage = ArrayBuffer[SflowRecord](SflowRecord(params(0)), SflowRecord(params(1)))


  def foo() = {
    val hz1 = getHazelcastInstance

    val imap = hz1.getMap[String, SflowRecord](hazelcastMapName)

    storage.foreach(sflowRecord =>
      imap.put(uuid, sflowRecord)
    )


    val tracker = hz1.getJobTracker("tracker")
    val source = KeyValueSource.fromMap[String, SflowRecord] (imap)
    val job = tracker.newJob(source)
    val future = job.mapper(new AsMapper()).reducer(new AsReducerFactory()).submit()
    val result = future.get()
  }
}