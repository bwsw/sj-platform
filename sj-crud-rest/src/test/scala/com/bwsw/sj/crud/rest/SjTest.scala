package com.bwsw.sj.crud.rest

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.{InstanceMetadata, RegularInstanceMetadata, Task, ExecutionPlan}

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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

  def main(args: Array[String]) = {
    val instance = serializer.deserialize[RegularInstanceMetadata](instanceJson)
    val newInstance = serializer.serialize(createPlan(instance))
    //println(newInstance)
  }

  case class InputStream(name: String, mode: String, partitionsCount: Int)

  case class StreamProcess(currentPartition: Int, countFreePartitions: Int)

  def createPlan(instance: InstanceMetadata): InstanceMetadata = {
    val inputs = instance.inputs.map { input =>
      val mode = getStreamMode(input)
      val name = input.replaceAll("/split|/full", "")
      InputStream(name, mode, getPartitionCount(name))
    }
    val tasks = (0 until instance.parallelism)
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
  }

}
