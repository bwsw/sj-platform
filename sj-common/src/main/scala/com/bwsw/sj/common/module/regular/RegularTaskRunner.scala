package com.bwsw.sj.common.module.regular

import java.io.File
import java.net.URLClassLoader
import java.util.UUID

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.RegularInstanceMetadata
import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.module.state.{StateStorage, RAMStateService}
import com.bwsw.sj.common.module.{PersistentBlockingQueue, SjTimer, TaskEnvironmentManager}
import com.bwsw.tstreams.agents.consumer.Offsets.{IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.producer.{BasicProducer, ProducerPolicies}

import scala.collection.mutable

/**
 * Object responsible for running a task of job
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner {

  def main(args: Array[String]) {
    val serializer = new JsonSerializer()
    val taskName = System.getenv("TASK_NAME")
    val moduleJar = new File(s"${System.getenv("MODULE_NAME")}.jar")

    val manager = new TaskEnvironmentManager()

    manager.downloadModuleJar(moduleJar)

    val regularInstanceMetadata = manager.getRegularInstanceMetadata(serializer)

    val specification = manager.getSpecification(serializer)

    val temporaryOutput = manager.getTemporaryOutput
    val moduleTimer = new SjTimer()

    val consumers = regularInstanceMetadata.executionPlan.tasks(taskName).inputs
      .map(x => manager.createConsumer(x._1, x._2, chooseOffsetPolicy(regularInstanceMetadata.startFrom))).toVector
    val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] = regularInstanceMetadata.outputs
      .map(x => (x, manager.createProducer(x))).toMap

    val classLoader = manager.getClassLoader(moduleJar.getAbsolutePath)

    //todo: stub for pub/sub
    val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue("temp")

    new Thread(new Runnable {
      def run() {
        var i = 0
        while (i < 7) {
          Thread.sleep(1000)
          blockingQueue.put(serializer.serialize(Transaction("s1", 0, UUID.randomUUID(), "test_consumer",
            List("wow!".getBytes,
              "wow!!".getBytes,
              "wow!!!".getBytes)
          )))
          i = i + 1
        }
      }
    }).start()

    runModule(moduleTimer,
      regularInstanceMetadata,
      blockingQueue,
      temporaryOutput,
      classLoader,
      specification.executorClass,
      producers,
      serializer,
      manager)
  }

  private def sendData(output: (String, (String, Any)), producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]]) {
    output._2._1 match {
      case "partitioned" =>
        val partitionedData = output._2._2.asInstanceOf[mutable.Map[Int, mutable.MutableList[Array[Byte]]]]
        partitionedData.foreach(y => {
          val transaction = producers(output._1).newTransaction(ProducerPolicies.errorIfOpen, y._1)
          y._2.foreach(transaction.send)
          transaction.close()
          println(s"stream: ${output._1}, number of elements: ${y._2.length}")
        })
      case "roundrobin" =>
        val transaction = producers(output._1).newTransaction(ProducerPolicies.errorIfOpen)
        val data = output._2._2.asInstanceOf[mutable.MutableList[Array[Byte]]]
        data.foreach(transaction.send)
        transaction.close()
        println(s"stream: ${output._1}, number of elements: ${data.length}")
    }
  }

  private def runModule(moduleTimer: SjTimer,
                        regularInstanceMetadata: RegularInstanceMetadata,
                        blockingQueue: PersistentBlockingQueue,
                        temporaryOutput: mutable.Map[String, (String, Any)],
                        classLoader: URLClassLoader,
                        pathToExecutor: String,
                        producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]],
                        serializer: JsonSerializer,
                        manager: TaskEnvironmentManager) = {
    regularInstanceMetadata.stateManagement match {
      case "none" =>
        val moduleEnvironmentManager = new ModuleEnvironmentManager(
          regularInstanceMetadata.options,
          temporaryOutput,
          moduleTimer
        )

        val executor: RegularStreamingExecutor = classLoader.loadClass(pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

        executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
            while (true) {
              //todo: stub for publish subscribe
              val transaction: Transaction = serializer.deserialize[Transaction](blockingQueue.get())

              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              if (checkpointTimer.isTime) {
                //todo GroupCheckpoint
                executor.onCheckpoint()
                checkpointTimer.resetTimer()
                checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.resetTimer()
              }
            }
          case "every-nth" =>
            var countOfTransaction = 0
            while (true) {
              //todo: stub for publish subscribe
              val transaction: Transaction = serializer.deserialize[Transaction](blockingQueue.get())
              countOfTransaction += 1

              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              if (countOfTransaction == regularInstanceMetadata.checkpointInterval) {
                //todo GroupCheckpoint
                executor.onCheckpoint()
                countOfTransaction = 0
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.resetTimer()
              }
            }
        }

      case "ram" =>
        var countOfCheckpoints = 0
        val taskName = System.getenv("TASK_NAME")

        val stateService = new RAMStateService(
          manager.createProducer(taskName),
          manager.createConsumer(taskName, List(0, 0), Newest))

        val moduleEnvironmentManager = new StatefulModuleEnvironmentManager(
          new StateStorage(stateService),
          regularInstanceMetadata.options,
          temporaryOutput,
          moduleTimer
        )

//        val executor = classLoader.loadClass(pathToExecutor)
//          .getConstructor(classOf[ModuleEnvironmentManager])
//          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

        val executor = new Executor(moduleEnvironmentManager)

        executor.init()
//        val a = stateService.stateVariables.foreach(x => println(x._1 + " " + x._2.toString))
//        val t = 1

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
            while (true) {
              //todo: stub for publish subscribe
              val transaction: Transaction = serializer.deserialize[Transaction](blockingQueue.get())

              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              val b = stateService.get("sum").asInstanceOf[Int]
              println(b)

              if (checkpointTimer.isTime) {
                // todo GroupCheckpoint
                if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                  stateService.checkpoint()
                  countOfCheckpoints += 1
                } else {
                  stateService.fullCheckpoint()
                  countOfCheckpoints = 0
                }
                executor.onCheckpoint()
                checkpointTimer.resetTimer()
                checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.resetTimer()
              }
            }
          case "every-nth" =>
            var countOfTransaction = 0
            while (true) {
              //todo: stub for publish subscribe
              val transaction: Transaction = serializer.deserialize[Transaction](blockingQueue.get())
              countOfTransaction += 1

              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              if (countOfTransaction == regularInstanceMetadata.checkpointInterval) {
                //todo GroupCheckpoint
                if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                  stateService.checkpoint()
                  countOfCheckpoints += 1
                } else {
                  stateService.fullCheckpoint()
                  countOfCheckpoints = 0
                }
                executor.onCheckpoint()
                countOfTransaction = 0
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.resetTimer()
              }
            }
        }
    }

  }

  def chooseOffsetPolicy(startFrom: String): IOffset = {
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      //todo add two cases for date and UUID
    }
  }
}

class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

  override def init(): Unit = {
    try {
      val state = manager.getState

      println(s"getting state")
      state.set("sum", 0)
    } catch {
      case _: java.lang.Exception => println("exception")
    }

    println("new init")
  }

  override def finish(): Unit = ???

  override def onCheckpoint(): Unit = {
    println("onCheckpoint")
  }

  override def run(transaction: Transaction): Unit = {
    val output = manager.getPartitionedOutput("s3")
    val state = manager.getState
    var sum = state.get("sum").asInstanceOf[Int]
    transaction.data.foreach(x => {
      output.put(x, 0)
      output.put(x, 1)
      output.put(x, 2)
    })
    sum += 1
    state.set("sum", sum)
    println("in run")
  }

  override def onTimer(): Unit = {
    println("onTimer")
  }
}
