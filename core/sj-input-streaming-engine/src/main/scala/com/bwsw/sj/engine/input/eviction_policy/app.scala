package com.bwsw.sj.engine.input.eviction_policy

import java.util.concurrent.atomic.AtomicInteger

import com.hazelcast.config.{EvictionPolicy, _}
import com.hazelcast.core.Hazelcast
import com.hazelcast.mapreduce._

import scala.collection.JavaConverters._




object ap extends App {
  val hazelcastMapName = "name3"

  val config = new XmlConfigBuilder().build()

  val tcpIpConfig = new TcpIpConfig()
  val hosts = Array("192.168.1.225").toList.asJava
  tcpIpConfig.setMembers(hosts).setEnabled(true)

  val joinConfig = new JoinConfig()
  joinConfig.setMulticastConfig(new MulticastConfig().setEnabled(false))
  joinConfig.setTcpIpConfig(tcpIpConfig)

  val networkConfig = new NetworkConfig()
  networkConfig.setJoin(joinConfig)

  config.setNetworkConfig(networkConfig)
    .getMapConfig(hazelcastMapName)


  val hz1 = Hazelcast.newHazelcastInstance(config)

  val imap = hz1.getMap[Int, Int](hazelcastMapName)

  (3 to 4).foreach(i => imap.put(i, i))

  val tracker = hz1.getJobTracker("tracker")
  val source = KeyValueSource.fromMap[Int, Int](imap)
  val job = tracker.newJob(source)


  val future = job.mapper(new MyMapper()).reducer(new MyReducerFactory()).submit()
  val result = future.get()

  println(result.get(1))

  println(imap.values())
}


class MyMapper extends Mapper[Int,Int, Int, Int] {


   override def map(key: Int, value: Int, context: Context[Int, Int]) = {
    context.emit(1, value)
  }
}


class MyReducerFactory extends ReducerFactory[Int, Int, Int] {
  override def newReducer(key: Int) = {
    new MyReducer()
  }
}


class MyReducer extends Reducer[Int, Int] {
  val sum: AtomicInteger = new AtomicInteger(0)

  override def reduce(value: Int) = {
    sum.addAndGet(value)
  }

  override def finalizeReduce(): Int = {
    sum.get()
  }
}