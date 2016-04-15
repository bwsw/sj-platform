package com.bwsw.sj.common.module.regular

import java.net.URLClassLoader
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit._

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.RegularInstanceMetadata
import com.bwsw.sj.common.module.entities.{TaskParameters, Transaction}
import com.bwsw.sj.common.module.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.module.state.DefaultModuleStateStorage
import com.bwsw.sj.common.module.{SjTimer, TaskEnvironmentManager}
import com.bwsw.tstreams.agents.consumer.BasicConsumer
import com.bwsw.tstreams.agents.producer.BasicProducer

import scala.collection.mutable

/**
 * Object responsible for running a task of job
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner {

  def main(args: Array[String]) {
    val serializer = new JsonSerializer()
    val taskParameters = serializer.deserialize[TaskParameters](args(0))
    //todo: обращаться к ксюшиному ресту для получения
    val regularInstanceMetadata = taskParameters.instanceMetadata.asInstanceOf[RegularInstanceMetadata]

    val taskEnvironmentManager = new TaskEnvironmentManager()

    val temporaryOutput = mutable.Map(regularInstanceMetadata.outputs.map(x => (x, mutable.MutableList[Array[Byte]]())): _*)
    val moduleTimer = new SjTimer()

    val transactionQueue: ArrayBlockingQueue[Transaction] = new ArrayBlockingQueue[Transaction](taskParameters.queueSize, true)

    val consumers = taskParameters.inputsWithPartitionRange.map(x => taskEnvironmentManager.createConsumer(x._1, x._2)).toVector
    val producers = regularInstanceMetadata.outputs.map(x => (x, taskEnvironmentManager.createProducer(x))).toMap

    val classLoader = taskEnvironmentManager.getClassLoader(taskParameters.pathToJar)

    //todo: stub for publish subscribe
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

    runModule(moduleTimer, regularInstanceMetadata, transactionQueue, temporaryOutput, classLoader, taskParameters)
  }

  private def runModule(moduleTimer: SjTimer,
                        regularInstanceMetadata: RegularInstanceMetadata,
                        transactionQueue: ArrayBlockingQueue[Transaction],
                        temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                        classLoader: URLClassLoader,
                        taskParameters: TaskParameters) = {
    regularInstanceMetadata.stateManagement match {
      case "none" =>
        val moduleEnvironmentManager = new ModuleEnvironmentManager(
          regularInstanceMetadata.options,
          regularInstanceMetadata.outputs,
          temporaryOutput,
          moduleTimer
        )

        val executor: RegularStreamingExecutor = classLoader.loadClass(taskParameters.pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

        executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
            while (true) {
              val transaction: Transaction = transactionQueue.poll(taskParameters.transactionTimeout, MILLISECONDS)
              if (transaction != null) {
                executor.run(transaction)
                temporaryOutput.foreach(x => println(s"producer: ${x._1}, number of elements: ${x._2.length}"))
                if (checkpointTimer.isTime) {
                  //todo GroupCheckpoint
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
                  //todo GroupCheckpoint
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

      case "ram" =>
        var countOfCheckpoints = 0

        val moduleStateStorage = new DefaultModuleStateStorage(
          new BasicProducer[Array[Byte], Array[Byte]]("stub producer", null, null),
          new BasicConsumer[Array[Byte], Array[Byte]]("stub consumer", null, null))

        val moduleEnvironmentManager = new StatefulModuleEnvironmentManager(
          moduleStateStorage,
          regularInstanceMetadata.options,
          regularInstanceMetadata.outputs,
          temporaryOutput,
          moduleTimer
        )

        val executor = classLoader.loadClass(taskParameters.pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

        executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
            while (true) {
              val transaction: Transaction = transactionQueue.poll(taskParameters.transactionTimeout, MILLISECONDS)
              if (transaction != null) {
                executor.run(transaction)
                temporaryOutput.foreach(x => println(s"producer: ${x._1}, number of elements: ${x._2.length}"))
                if (checkpointTimer.isTime) {
                  // todo GroupCheckpoint
                  if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                    moduleStateStorage.checkpoint()
                    countOfCheckpoints += 1
                  } else {
                    moduleStateStorage.fullCheckpoint()
                    countOfCheckpoints = 0
                  }
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

  }

  private def startExecutionLogic(transactionQueue: ArrayBlockingQueue[Transaction],
                                  regularInstanceMetadata: RegularInstanceMetadata,
                                  temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                                  executor: RegularStreamingExecutor,
                                  transactionTimeout: Long,
                                  moduleTimer: SjTimer,
                                  stateCheckpoint: () => Unit) = {


  }
}