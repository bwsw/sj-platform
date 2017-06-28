/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.core.simulation.input.mocks

import java.util
import java.util.concurrent.TimeUnit

import com.bwsw.common.hazelcast.HazelcastConfig
import com.bwsw.sj.common.utils.EngineLiterals
import com.hazelcast.core._
import com.hazelcast.map.listener.{MapListener, MapPartitionLostListener}
import com.hazelcast.map.{EntryProcessor, MapInterceptor}
import com.hazelcast.mapreduce.JobTracker
import com.hazelcast.mapreduce.aggregation.{Aggregation, Supplier}
import com.hazelcast.monitor.LocalMapStats
import com.hazelcast.query.Predicate

import scala.collection.mutable

/**
  * Mock for [[IMap]]
  *
  * @param config configuration parameters for hazelcast cluster
  * @author Pavel Tomskikh
  */
abstract class HazelcastMapMock(config: HazelcastConfig) extends IMap[String, String] {

  val map: mutable.Map[String, HazelcastMapValue] = mutable.Map()

  protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean]
  private val ttlMillis: Long = config.ttlSeconds * 1000

  override def containsKey(key: Any): Boolean = {
    evictExpiredEntries()
    map.contains(key.asInstanceOf[String])
  }

  override def put(key: String, value: String): String = {
    val oldValue = map.get(key)
    val hits = oldValue.map(_.hits).getOrElse(0)
    map.put(key, HazelcastMapValue(value, hits + 1, System.currentTimeMillis))
    evict()
    oldValue.map(_.value).orNull
  }

  override def set(key: String, value: String): Unit =
    put(key, value)

  /**
    * Performs eviction
    */
  def evict(): Unit = {
    evictExpiredEntries()

    if (map.size > config.maxSize) {
      evictionComparator.foreach { comparator =>
        val keysForEviction = map.toSeq
          .sortWith((v1, v2) => comparator(v1._2, v2._2))
          .drop(config.maxSize).map(_._1)
        keysForEviction.foreach(map.remove)
      }
    }
  }

  /**
    * Evicts expired entries
    */
  def evictExpiredEntries(): Unit = {
    if (ttlMillis > 0) {
      val currentTime = System.currentTimeMillis
      val expiredEntries = map.filter {
        case (_, value) =>
          value.lastAccessTime + ttlMillis < currentTime
      }.keysIterator
      expiredEntries.foreach(map.remove)
    }
  }

  override def equals(obj: Any): Boolean = obj match {
    case hazelcastMapMock: HazelcastMapMock => map == hazelcastMapMock.map
    case _ => super.equals(obj)
  }


  override def removeAsync(key: String): ICompletableFuture[String] = ???

  override def setAsync(key: String, value: String): ICompletableFuture[Void] = ???

  override def setAsync(key: String, value: String, ttl: Long, timeunit: TimeUnit): ICompletableFuture[Void] = ???

  override def putTransient(key: String, value: String, ttl: Long, timeunit: TimeUnit): Unit = ???

  override def containsValue(value: Any): Boolean = ???

  override def put(key: String, value: String, ttl: Long, timeunit: TimeUnit): String = ???

  override def evict(key: String): Boolean = ???

  override def isLocked(key: String): Boolean = ???

  override def lock(key: String): Unit = ???

  override def lock(key: String, leaseTime: Long, timeUnit: TimeUnit): Unit = ???

  override def removeInterceptor(id: String): Unit = ???

  override def unlock(key: String): Unit = ???

  override def tryPut(key: String, value: String, timeout: Long, timeunit: TimeUnit): Boolean = ???

  override def executeOnEntries(entryProcessor: EntryProcessor[_, _]): util.Map[String, AnyRef] = ???

  override def executeOnEntries(entryProcessor: EntryProcessor[_, _], predicate: Predicate[_, _]): util.Map[String, AnyRef] = ???

  override def addLocalEntryListener(listener: MapListener): String = ???

  override def addLocalEntryListener(listener: EntryListener[_, _]): String = ???

  override def addLocalEntryListener(listener: MapListener, predicate: Predicate[String, String], includeValue: Boolean): String = ???

  override def addLocalEntryListener(listener: EntryListener[_, _], predicate: Predicate[String, String], includeValue: Boolean): String = ???

  override def addLocalEntryListener(listener: MapListener, predicate: Predicate[String, String], key: String, includeValue: Boolean): String = ???

  override def addLocalEntryListener(listener: EntryListener[_, _], predicate: Predicate[String, String], key: String, includeValue: Boolean): String = ???

  override def entrySet(): util.Set[util.Map.Entry[String, String]] = ???

  override def entrySet(predicate: Predicate[_, _]): util.Set[util.Map.Entry[String, String]] = ???

  override def forceUnlock(key: String): Unit = ???

  override def removeEntryListener(id: String): Boolean = ???

  override def addIndex(attribute: String, ordered: Boolean): Unit = ???

  override def addEntryListener(listener: MapListener, includeValue: Boolean): String = ???

  override def addEntryListener(listener: EntryListener[_, _], includeValue: Boolean): String = ???

  override def addEntryListener(listener: MapListener, key: String, includeValue: Boolean): String = ???

  override def addEntryListener(listener: EntryListener[_, _], key: String, includeValue: Boolean): String = ???

  override def addEntryListener(listener: MapListener, predicate: Predicate[String, String], includeValue: Boolean): String = ???

  override def addEntryListener(listener: EntryListener[_, _], predicate: Predicate[String, String], includeValue: Boolean): String = ???

  override def addEntryListener(listener: MapListener, predicate: Predicate[String, String], key: String, includeValue: Boolean): String = ???

  override def addEntryListener(listener: EntryListener[_, _], predicate: Predicate[String, String], key: String, includeValue: Boolean): String = ???

  override def tryLock(key: String): Boolean = ???

  override def tryLock(key: String, time: Long, timeunit: TimeUnit): Boolean = ???

  override def tryLock(key: String, time: Long, timeunit: TimeUnit, leaseTime: Long, leaseTimeunit: TimeUnit): Boolean = ???

  override def addInterceptor(interceptor: MapInterceptor): String = ???

  override def submitToKey(key: String, entryProcessor: EntryProcessor[_, _], callback: ExecutionCallback[_]): Unit = ???

  override def submitToKey(key: String, entryProcessor: EntryProcessor[_, _]): ICompletableFuture[_] = ???

  override def values(): util.Collection[String] = ???

  override def values(predicate: Predicate[_, _]): util.Collection[String] = ???

  override def loadAll(replaceExistingValues: Boolean): Unit = ???

  override def loadAll(keys: util.Set[String], replaceExistingValues: Boolean): Unit = ???

  override def delete(key: Any): Unit = ???

  override def localKeySet(): util.Set[String] = ???

  override def localKeySet(predicate: Predicate[_, _]): util.Set[String] = ???

  override def remove(key: Any): String = ???

  override def aggregate[SuppliedValue, Result](supplier: Supplier[String, String, SuppliedValue], aggregation: Aggregation[String, SuppliedValue, Result]): Result = ???

  override def aggregate[SuppliedValue, Result](supplier: Supplier[String, String, SuppliedValue], aggregation: Aggregation[String, SuppliedValue, Result], jobTracker: JobTracker): Result = ???

  override def evictAll(): Unit = ???

  override def flush(): Unit = ???

  override def putAsync(key: String, value: String): ICompletableFuture[String] = ???

  override def putAsync(key: String, value: String, ttl: Long, timeunit: TimeUnit): ICompletableFuture[String] = ???

  override def tryRemove(key: String, timeout: Long, timeunit: TimeUnit): Boolean = ???

  override def getLocalMapStats: LocalMapStats = ???

  override def putAll(m: util.Map[_ <: String, _ <: String]): Unit = ???

  override def executeOnKey(key: String, entryProcessor: EntryProcessor[_, _]): AnyRef = ???

  override def get(key: Any): String = ???

  override def getEntryView(key: String): EntryView[String, String] = ???

  override def removePartitionLostListener(id: String): Boolean = ???

  override def keySet(): util.Set[String] = ???

  override def keySet(predicate: Predicate[_, _]): util.Set[String] = ???

  override def set(key: String, value: String, ttl: Long, timeunit: TimeUnit): Unit = ???

  override def getAll(keys: util.Set[String]): util.Map[String, String] = ???

  override def addPartitionLostListener(listener: MapPartitionLostListener): String = ???

  override def clear(): Unit = ???

  override def getAsync(key: String): ICompletableFuture[String] = ???

  override def executeOnKeys(keys: util.Set[String], entryProcessor: EntryProcessor[_, _]): util.Map[String, AnyRef] = ???

  override def putIfAbsent(key: String, value: String, ttl: Long, timeunit: TimeUnit): String = ???

  override def size(): Int = ???

  override def isEmpty: Boolean = ???

  override def getName: String = ???

  override def destroy(): Unit = ???

  override def getPartitionKey: String = ???

  override def getServiceName: String = ???
}

object HazelcastMapMock {
  def apply(config: HazelcastConfig): HazelcastMapMock = {
    config.evictionPolicy match {
      case EngineLiterals.lruDefaultEvictionPolicy => new LruEvictionHazelcastMapMock(config)
      case EngineLiterals.lfuDefaultEvictionPolicy => new LfuEvictionHazelcastMapMock(config)
      case _ => new NoneEvictionHazelcastMapMock(config)
    }
  }
}

case class HazelcastMapValue(value: String, hits: Int, lastAccessTime: Long) {
  override def equals(obj: scala.Any): Boolean = obj match {
    case hazelcastMapValue: HazelcastMapValue =>
      value == hazelcastMapValue.value && hits == hazelcastMapValue.hits
    case _ => super.equals(obj)
  }
}

/**
  * Mock for [[IMap]] with "Least Recently Used" eviction policy
  */
class LruEvictionHazelcastMapMock(config: HazelcastConfig) extends HazelcastMapMock(config) {
  override protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean] =
    Some((v1: HazelcastMapValue, v2: HazelcastMapValue) => v1.lastAccessTime > v2.lastAccessTime)
}

/**
  * Mock for [[IMap]] with "Least Frequently Used" eviction policy
  */
class LfuEvictionHazelcastMapMock(config: HazelcastConfig) extends HazelcastMapMock(config) {
  override protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean] =
    Some((v1: HazelcastMapValue, v2: HazelcastMapValue) => v1.hits > v2.hits)
}

/**
  * Mock for [[IMap]] without eviction policy
  */
class NoneEvictionHazelcastMapMock(config: HazelcastConfig) extends HazelcastMapMock(config) {
  override protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean] = None
}
