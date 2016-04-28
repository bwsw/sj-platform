package com.bwsw.sj.crud.rest

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.ConnectionRepository
import com.bwsw.sj.common.entities._
import com.mongodb.casbah.MongoClient


import scala.collection.immutable.IndexedSeq
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import com.mongodb.casbah.Imports._

/**
  * Created: 4/14/16
  *
  * @author Kseniya Tomskikh
  */
object SjTest {

  val serializer = new JsonSerializer

  private val instanceJson = "{\n  " +
    "\"uuid\" : \"qwe-123-dsf\",\n  " +
    "\"name\" : \"instance-test\",\n  " +
    "\"description\" : \"\",\n  " +
    "\"inputs\" : [\"s1/split\", \"s2/full\", \"s3/split\"],\n  " +
    "\"outputs\" : [\"s2\", \"s3\"],\n  " +
    "\"checkpoint-mode\" : \"time-interval\",\n  " +
    "\"checkpoint-interval\" : 100,\n  " +
    "\"state-management\" : \"ram\",\n  " +
    "\"state-full-checkpoint\" : 5,\n  " +
    "\"parallelism\" : 10,\n  " +
    "\"options\" : {\"11\" : \"3e2ew\"},\n  " +
    "\"start-from\" : \"oldest\",\n  " +
    "\"per-task-cores\" : 2,\n  " +
    "\"per-task-ram\" : 128,\n  " +
    "\"jvm-options\" : {\"a\" : \"dsf\"}\n}"

  private val spec = "{\n  \"name\": \"com.bwsw.sj.stub\",\n  \"description\": \"Stub module by BW\",\n  \"version\": \"0.1\",\n  \"author\": \"Ksenia Mikhaleva\",\n  \"license\": \"Apache 2.0\",\n  \"inputs\": {\n    \"cardinality\": [\n      1,\n      1\n    ],\n    \"types\": [\n      \"stream.t-stream\"\n    ]\n  },\n  \"outputs\": {\n    \"cardinality\": [\n      1,\n      1\n    ],\n    \"types\": [\n      \"stream.t-stream\"\n    ]\n  },\n  \"module-type\": \"regular-streaming\",\n  \"engine\": \"streaming\",\n  \"options\": {\n    \"opt\": 1\n  },\n  \"validator-class\": \"com.bwsw.sj.stub.Validator\",\n  \"executor-class\": \"com.bwsw.sj.stub.Executor\"\n}"

  serializer.setIgnoreUnknown(true)


  def main(args: Array[String]) = {
    /*val stream = new SjStream
    stream.name = "s1"
    stream.description = ""
    stream.partitions = Array("1", "2", "3", "4", "5")
    stream.generator = Array("global", "service-zk://zk_service", "3")
    val dao = ConnectionRepository.getStreamService
    dao.save(stream)
    val res = dao.getAll
    println(serializer.serialize(res))*/
    val dao = ConnectionRepository.getFileMetadataService
    val res = dao.getAll
    println(serializer.serialize(res))
  }

  def tt() = {
    val specification = serializer.deserialize[Specification](spec)
    println(specification.moduleType)

    /*val allElems = collection.map(o => serializer.deserialize[RegularInstanceMetadata](o.toString)).toSeq
    /*allElems.foreach(println)
    val elems = collection.find("parallelism" $eq "10").map(_.toString).map(serializer.deserialize[ShortInstanceMetadata])
    if (elems.nonEmpty) {
      elems.foreach(println)
    } else {
      println("fail")
    }*/
    val res = retrieve(Map("parallelism" -> 10, "module-type" -> "regular-streaming"))
    res.foreach(println)*/
    /*val instance = serializer.deserialize[RegularInstanceMetadata](instanceJson)
    val newInstance = serializer.serialize(createPlan(instance))*/
    //println(newInstance)
  }


  case class Parameter(name: String, value: Any)

  def retrieve(parameters: Map[String, Any]) = {
    //parameters.map(x => (x.name $eq x.value))
    //collection.find(new MongoDBObject(parameters)).map(o => serializer.deserialize[ShortInstanceMetadata](o.toString)).toSeq
  }


  case class InputStream(name: String, mode: String, partitionsCount: Int)

  case class StreamProcess(currentPartition: Int, countFreePartitions: Int)

  /*def createPlan(instance: RegularInstanceMetadata): RegularInstanceMetadata = {
    val inputs = instance.inputs.map { input =>
      val mode = getStreamMode(input)
      val name = input.replaceAll("/split|/full", "")
      InputStream(name, mode, getPartitionCount(name))
    }
    var parallelism = 0
    instance.parallelism match {
      case i: Int => parallelism = i
      case s: String => parallelism = 0
    }
    val tasks = (0 until parallelism)
      .map(x => instance.uuid + "_task" + x)
      .map(x => x -> inputs)
    val executionPlan = mutable.Map[String, Task]()
    val streams: mutable.Map[String, StreamProcess] = mutable.Map(inputs.map(x => x.name -> StreamProcess(0, x.partitionsCount)).toSeq: _*)

    var tasksNotProcessed = tasks.size
    tasks.foreach { task =>
      val list = task._2.map { inputStream =>
        val stream = streams.get(inputStream.name).get
        val countFreePartitions = stream.countFreePartitions
        val startPartition = stream.currentPartition
        var endPartition = startPartition + countFreePartitions
        inputStream.mode match {
          case "full" => endPartition = startPartition + countFreePartitions
          case "split" =>
            val cntTaskStreamPartitions = countFreePartitions / tasksNotProcessed
            streams.update(inputStream.name, StreamProcess(startPartition + cntTaskStreamPartitions, countFreePartitions - cntTaskStreamPartitions))
            if (Math.abs(cntTaskStreamPartitions - countFreePartitions) >= cntTaskStreamPartitions) {
              endPartition = startPartition + cntTaskStreamPartitions
            }
        }

        inputStream.name -> List(startPartition, endPartition - 1)
      }
      tasksNotProcessed -= 1
      executionPlan.put(task._1, Task(mutable.Map(list.toSeq: _*)))
    }
    executionPlan.foreach(println)
    instance.executionPlan = ExecutionPlan(executionPlan)
    instance
  }

  def getStreamMode(name: String) = {
    name.substring(name.lastIndexOf("/") + 1)
  }

  def getPartitionCount(name: String) = {
    val map = Map("s1" -> 11, "s2" -> 12, "s3" -> 15)
    map.get(name).get
  }

  def checkJsonOfExecutionPlan() = {
    val tasks = mutable.Map("instance_task_0" -> Task(mutable.Map("s1" -> List(1, 2), "s2" -> List(3, 4))))
    val executionPlan = ExecutionPlan(tasks)
    val json = serializer.serialize(executionPlan)
    println(json)
  }*/

}
