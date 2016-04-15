package com.bwsw.sj.common.module.regular

import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit._

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.RegularInstanceMetadata
import com.bwsw.sj.common.module.entities.{TaskParameters, Transaction}
import com.bwsw.sj.common.module.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.module.{SjTimer, TaskEnvironmentManager}

import scala.collection.mutable

/**
 * Object responsible for running a task of job
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner extends App {

  val serializer = new JsonSerializer()
  val taskParameters = serializer.deserialize[TaskParameters](args(0))
  //обращаться к ксюшиному ресту для получения
  val regularInstanceMetadata = taskParameters.instanceMetadata.asInstanceOf[RegularInstanceMetadata]

  val taskEnvironmentManager = new TaskEnvironmentManager()

  val temporaryOutput = mutable.Map(regularInstanceMetadata.outputs.map(x => (x, mutable.MutableList[Array[Byte]]())): _*)
  val moduleTimer = new SjTimer()
  val moduleEnvironmentManager = chooseModuleEnvironmentManager(regularInstanceMetadata.stateManagement)
  val transactionQueue = new ArrayBlockingQueue[Transaction](taskParameters.queueSize, true)

  val consumers = taskParameters.inputsWithPartitionRange.map(x => taskEnvironmentManager.createConsumer(x._1, x._2)).toVector
  val producers = regularInstanceMetadata.outputs.map(x => (x, taskEnvironmentManager.createProducer(x))).toMap

  val classLoader = taskEnvironmentManager.getClassLoader(taskParameters.pathToJar)
  val executor = classLoader.loadClass(taskParameters.pathToExecutor)
    .getConstructor(classOf[ModuleEnvironmentManager])
    .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

  executor.init()

  new Thread(new Runnable {
    def run() {
      wait(2000)
      transactionQueue.add(Transaction("test_stream", 0, UUID.randomUUID(), "test_consumer",
        List(UUID.randomUUID().toString.getBytes,
          UUID.randomUUID().toString.getBytes,
          UUID.randomUUID().toString.getBytes)
      ))
    }
  }).start()

  private def runModule() = {
    regularInstanceMetadata.checkpointMode match {
      case "time-interval" =>
        val checkpointTimer = new SjTimer()
        checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
        //если бы было можно подписаться на consumer, то по появлению новой транзакции - выполнялось следующее:
        while (true) {
          val transaction: Transaction = transactionQueue.poll(taskParameters.transactionTimeout, MILLISECONDS)
          if (transaction != null) {
            executor.run(transaction)
            temporaryOutput.foreach(x => println(s"producer: ${x._1}, number of elements: ${x._2.length}"))
            if (checkpointTimer.isTime) {
              //GroupCheckpoint
              executor.onCheckpoint()
              checkpointTimer.resetTimer()
              checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
            }
            temporaryOutput.foreach(x => x._2.clear())
          }
          if (moduleTimer.isTime) {
            executor.onTimer()
            moduleTimer.resetTimer()
          }
        }
      case "every-nth" =>
        var countOfTransaction = 0
        while (true) {
          val transaction: Transaction = transactionQueue.poll(taskParameters.transactionTimeout, MILLISECONDS)
          if (transaction != null) {
            countOfTransaction += 1
            executor.run(transaction)
            temporaryOutput.foreach(x => println(s"producer: ${x._1}, number of elements: ${x._2.length}"))
            if (countOfTransaction == regularInstanceMetadata.checkpointInterval) {
              //GroupCheckpoint
              executor.onCheckpoint()
              countOfTransaction = 0
            }
            temporaryOutput.foreach(x => x._2.clear())
          }
          if (moduleTimer.isTime) {
            executor.onTimer()
            moduleTimer.resetTimer()
          }
        }
    }
  }

  private def chooseModuleEnvironmentManager(stateManagement: String) = {
    stateManagement match {
      case "none" => new ModuleEnvironmentManager(
        regularInstanceMetadata.options,
        regularInstanceMetadata.outputs,
        temporaryOutput,
        moduleTimer
      )

      case "ram" => new StatefulModuleEnvironmentManager(
        taskEnvironmentManager.getStateStorage(regularInstanceMetadata.stateManagement),
        regularInstanceMetadata.options,
        regularInstanceMetadata.outputs,
        temporaryOutput,
        moduleTimer
      )
    }
  }
}


//object TEST extends App {
//val serializer = new ObjectSerializer()
//  val someObject1 = Map("test" -> new test(), "sum" -> 0, "put" -> Some("hello"))
//  val a = serializer.serialize(someObject1)
//val b = serializer.deserialize(a).asInstanceOf[Map[String, Any]]
//
//  println(b("test").asInstanceOf[test].a.sd)
//}
//
//class test extends java.io.Serializable {
//  val s = 5
//  val a = new test1()
//}
//
//class test1 extends java.io.Serializable {
//  val sd = 6
//}