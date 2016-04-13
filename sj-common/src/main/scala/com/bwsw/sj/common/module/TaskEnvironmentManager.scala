package com.bwsw.sj.common.module

import java.net.{URL, URLClassLoader}

import com.bwsw.tstreams.agents.consumer.BasicConsumer
import com.bwsw.tstreams.agents.producer.BasicProducer

/**
 * Class allowing to manage environment of task
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

class TaskEnvironmentManager() {

  def getClassLoader(pathToJar: String) = {
    val classLoaderUrls = Array(new URL(pathToJar))

    new URLClassLoader(classLoaderUrls)

  }

  def createConsumer(streamName: String, partitions: List[Int]): BasicConsumer[Array[Byte], Array[Byte]] = {

    new BasicConsumer("consumer for " + streamName, null, null)
  }

  def createProducer(streamName: String) = {

    new BasicProducer[Array[Byte], Array[Byte]]("producer for " + streamName, null, null)
  }
}
