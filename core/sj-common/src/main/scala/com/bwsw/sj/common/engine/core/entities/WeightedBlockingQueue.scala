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
package com.bwsw.sj.common.engine.core.entities

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable
import scala.util.Try

/**
  * Blocking queue for keeping weighted elements
  *
  * @author Pavel Tomskikh
  */
class WeightedBlockingQueue[+T <: Weighted](lowWatermark: Int) {
  private val elements: mutable.Queue[Weighted] = mutable.Queue.empty[Weighted]
  private var weight: Int = 0
  private val lock: ReentrantLock = new ReentrantLock(true)
  private val notFull = lock.newCondition()
  private val notEmpty = lock.newCondition()

  /**
    * Inserts the specified element at the tail of this queue, waiting for space to become available
    * if a [[weight]] is larger that [[lowWatermark]].
    *
    * @param element specified element
    * @tparam E type of specified element
    */
  def put[E >: T <: Weighted](element: E): Unit = {
    callSynchronously(() => {
      while (weight >= lowWatermark)
        notFull.await()

      enqueue(element)
    })
  }

  /**
    * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary for an element
    * to become available.
    *
    * @param timeout how long to wait before giving up, in milliseconds
    * @return the head of this queue, or None if the specified waiting time elapses before an element is available
    */
  def poll(timeout: Long): Option[T] = {
    var nanos = TimeUnit.MILLISECONDS.toNanos(timeout)

    callSynchronously(() => {
      while (weight == 0 && nanos > 0)
        nanos = notEmpty.awaitNanos(nanos)

      dequeue()
    })
  }

  def isEmpty(): Boolean =
    callSynchronously(() => elements.isEmpty)

  /**
    * Returns the sum of sizes of all elements that stored in the queue
    *
    * @return the sum of sizes of all elements that stored in the queue
    */
  def getWeight(): Int =
    callSynchronously(() => weight)

  /**
    * Returns count of elements that stored in the queue
    *
    * @return count of elements that stored in the queue
    */
  def getCount(): Int =
    callSynchronously(() => elements.size)


  private def enqueue[E >: T <: Weighted](element: E): Unit = {
    weight += element.weight
    elements.enqueue(element)
    notEmpty.signal()
  }

  private def dequeue(): Option[T] = {
    val maybeElement = elements.dequeueFirst(_ => true)
    maybeElement.foreach(weight -= _.weight)

    if (weight < lowWatermark)
      notFull.signal()

    maybeElement.map(_.asInstanceOf[T])
  }

  private def callSynchronously[Y](f: () => Y): Y = {
    lock.lockInterruptibly()
    val result = Try(f())
    lock.unlock()

    result.get
  }
}
