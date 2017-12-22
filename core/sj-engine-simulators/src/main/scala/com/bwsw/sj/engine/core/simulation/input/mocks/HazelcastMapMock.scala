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
import java.util.Map
import java.util.concurrent.TimeUnit

import com.bwsw.common.hazelcast.HazelcastConfig
import com.bwsw.sj.common.utils.EngineLiterals
import com.hazelcast.core._
import com.hazelcast.map.{EntryProcessor, MapInterceptor}
import com.hazelcast.map.listener.{MapListener, MapPartitionLostListener}
import com.hazelcast.mapreduce.JobTracker
import com.hazelcast.mapreduce.aggregation.{Aggregation, Supplier}
import com.hazelcast.monitor.LocalMapStats
import com.hazelcast.query.Predicate

import scala.collection.mutable

/**
  * Mock for [[com.hazelcast.core.IMap]]
  *
  * @param config configuration parameters for hazelcast cluster
  * @author Pavel Tomskikh
  */
abstract class HazelcastMapMock(config: HazelcastConfig) extends IMap[String, String] {

  private val map: mutable.Map[String, HazelcastMapValue] = mutable.Map()

  protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean]
  private val ttlMillis: Long = config.ttlSeconds * 1000

  override def containsKey(key: Any): Boolean = {
    evictExpiredEntries()
    map.contains(key.asInstanceOf[String])
  }

  override def set(key: String, value: String): Unit = {
    val hits = map.get(key).map(_.hits).getOrElse(0)
    map.put(key, HazelcastMapValue(hits + 1, System.currentTimeMillis, value))
    evict()
  }

  override def put(key: String, value: String): String = {
    val previousValue = map.get(key).map(_.value).orNull
    set(key, value)

    previousValue
  }

  override def putIfAbsent(key: String, value: String): String = {
    if (containsKey(key)) map(key).value
    else put(key, value)
  }

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


  override def removeAsync(k: String): ICompletableFuture[String] = ???

  override def setAsync(k: String, v: String): ICompletableFuture[Void] = ???

  override def setAsync(k: String, v: String, l: Long, timeUnit: TimeUnit): ICompletableFuture[Void] = ???

  override def putTransient(k: String, v: String, l: Long, timeUnit: TimeUnit): Unit = ???

  override def containsValue(o: scala.Any): Boolean = ???

  override def put(k: String, v: String, l: Long, timeUnit: TimeUnit): String = ???

  override def evict(k: String): Boolean = ???

  override def isLocked(k: String): Boolean = ???

  override def lock(k: String): Unit = ???

  override def lock(k: String, l: Long, timeUnit: TimeUnit): Unit = ???

  override def removeInterceptor(s: String): Unit = ???

  override def unlock(k: String): Unit = ???

  override def tryPut(k: String, v: String, l: Long, timeUnit: TimeUnit): Boolean = ???

  override def executeOnEntries(entryProcessor: EntryProcessor[_, _]): util.Map[String, AnyRef] = ???

  override def executeOnEntries(entryProcessor: EntryProcessor[_, _], predicate: Predicate[_, _]): util.Map[String, AnyRef] = ???

  override def addLocalEntryListener(mapListener: MapListener): String = ???

  override def addLocalEntryListener(entryListener: EntryListener[_, _]): String = ???

  override def addLocalEntryListener(mapListener: MapListener, predicate: Predicate[String, String], b: Boolean): String = ???

  override def addLocalEntryListener(entryListener: EntryListener[_, _], predicate: Predicate[String, String], b: Boolean): String = ???

  override def addLocalEntryListener(mapListener: MapListener, predicate: Predicate[String, String], k: String, b: Boolean): String = ???

  override def addLocalEntryListener(entryListener: EntryListener[_, _], predicate: Predicate[String, String], k: String, b: Boolean): String = ???

  override def entrySet(): util.Set[Map.Entry[String, String]] = ???

  override def entrySet(predicate: Predicate[_, _]): util.Set[Map.Entry[String, String]] = ???

  override def forceUnlock(k: String): Unit = ???

  override def removeEntryListener(s: String): Boolean = ???

  override def addIndex(s: String, b: Boolean): Unit = ???

  override def addEntryListener(mapListener: MapListener, b: Boolean): String = ???

  override def addEntryListener(entryListener: EntryListener[_, _], b: Boolean): String = ???

  override def addEntryListener(mapListener: MapListener, k: String, b: Boolean): String = ???

  override def addEntryListener(entryListener: EntryListener[_, _], k: String, b: Boolean): String = ???

  override def addEntryListener(mapListener: MapListener, predicate: Predicate[String, String], b: Boolean): String = ???

  override def addEntryListener(entryListener: EntryListener[_, _], predicate: Predicate[String, String], b: Boolean): String = ???

  override def addEntryListener(mapListener: MapListener, predicate: Predicate[String, String], k: String, b: Boolean): String = ???

  override def addEntryListener(entryListener: EntryListener[_, _], predicate: Predicate[String, String], k: String, b: Boolean): String = ???

  override def tryLock(k: String): Boolean = ???

  override def tryLock(k: String, l: Long, timeUnit: TimeUnit): Boolean = ???

  override def tryLock(k: String, l: Long, timeUnit: TimeUnit, l1: Long, timeUnit1: TimeUnit): Boolean = ???

  override def addInterceptor(mapInterceptor: MapInterceptor): String = ???

  override def submitToKey(k: String, entryProcessor: EntryProcessor[_, _], executionCallback: ExecutionCallback[_]): Unit = ???

  override def submitToKey(k: String, entryProcessor: EntryProcessor[_, _]): ICompletableFuture[_] = ???

  override def values(): util.Collection[String] = ???

  override def values(predicate: Predicate[_, _]): util.Collection[String] = ???

  override def loadAll(b: Boolean): Unit = ???

  override def loadAll(set: util.Set[String], b: Boolean): Unit = ???

  override def delete(o: scala.Any): Unit = ???

  override def localKeySet(): util.Set[String] = ???

  override def localKeySet(predicate: Predicate[_, _]): util.Set[String] = ???

  override def remove(o: scala.Any): String = ???

  override def aggregate[SuppliedValue, Result](supplier: Supplier[String, String, SuppliedValue], aggregation: Aggregation[String, SuppliedValue, Result]): Result = ???

  override def aggregate[SuppliedValue, Result](supplier: Supplier[String, String, SuppliedValue], aggregation: Aggregation[String, SuppliedValue, Result], jobTracker: JobTracker): Result = ???

  override def evictAll(): Unit = ???

  override def flush(): Unit = ???

  override def putAsync(k: String, v: String): ICompletableFuture[String] = ???

  override def putAsync(k: String, v: String, l: Long, timeUnit: TimeUnit): ICompletableFuture[String] = ???

  override def tryRemove(k: String, l: Long, timeUnit: TimeUnit): Boolean = ???

  override def getLocalMapStats: LocalMapStats = ???

  override def putAll(map: util.Map[_ <: String, _ <: String]): Unit = ???

  override def executeOnKey(k: String, entryProcessor: EntryProcessor[_, _]): AnyRef = ???

  override def get(o: scala.Any): String = ???

  override def getEntryView(k: String): EntryView[String, String] = ???

  override def removePartitionLostListener(s: String): Boolean = ???

  override def keySet(): util.Set[String] = ???

  override def keySet(predicate: Predicate[_, _]): util.Set[String] = ???

  override def set(k: String, v: String, l: Long, timeUnit: TimeUnit): Unit = ???

  override def getAll(set: util.Set[String]): util.Map[String, String] = ???

  override def addPartitionLostListener(mapPartitionLostListener: MapPartitionLostListener): String = ???

  override def clear(): Unit = ???

  override def getAsync(k: String): ICompletableFuture[String] = ???

  override def executeOnKeys(set: util.Set[String], entryProcessor: EntryProcessor[_, _]): util.Map[String, AnyRef] = ???

  override def putIfAbsent(k: String, v: String, l: Long, timeUnit: TimeUnit): String = ???

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

case class HazelcastMapValue(hits: Int, lastAccessTime: Long, value: String) {
  override def equals(obj: Any): Boolean = obj match {
    case hazelcastMapValue: HazelcastMapValue =>
      hits == hazelcastMapValue.hits &&
        value == hazelcastMapValue.value
    case _ => super.equals(obj)
  }
}

/**
  * Mock for [[com.hazelcast.core.IMap]] with "Least Recently Used" eviction policy
  */
class LruEvictionHazelcastMapMock(config: HazelcastConfig) extends HazelcastMapMock(config) {
  override protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean] =
    Some((v1: HazelcastMapValue, v2: HazelcastMapValue) => v1.lastAccessTime > v2.lastAccessTime)
}

/**
  * Mock for [[com.hazelcast.core.IMap]] with "Least Frequently Used" eviction policy
  */
class LfuEvictionHazelcastMapMock(config: HazelcastConfig) extends HazelcastMapMock(config) {
  override protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean] =
    Some((v1: HazelcastMapValue, v2: HazelcastMapValue) => v1.hits > v2.hits)
}

/**
  * Mock for [[com.hazelcast.core.IMap]] without eviction policy
  */
class NoneEvictionHazelcastMapMock(config: HazelcastConfig) extends HazelcastMapMock(config) {
  override protected val evictionComparator: Option[(HazelcastMapValue, HazelcastMapValue) => Boolean] = None
}
