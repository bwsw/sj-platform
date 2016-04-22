package com.bwsw.sj.common.module.regular

import java.io.File
import java.net.URLClassLoader

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.RegularInstanceMetadata
import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.module.state.{RAMStateService, StateStorage}
import com.bwsw.sj.common.module.{PersistentBlockingQueue, SjTimer, TaskEnvironmentManager}
import com.bwsw.tstreams.agents.consumer.Offsets.{IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.{BasicProducer, ProducerPolicies}

import scala.collection.mutable

/**
 * Object responsible for running a task of job that launches regular module
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

    val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue("temp")
    val checkpointGroup = new CheckpointGroup()

    //    val consumerWithSubscribes = regularInstanceMetadata.executionPlan.tasks(taskName).inputs
    //      .map(x => manager.createConsumer(x._1, x._2, chooseOffsetPolicy(regularInstanceMetadata.startFrom), blockingQueue)).toVector
    //
    //    consumerWithSubscribes.foreach(x => checkpointGroup.add(x.name, x))
    //    consumerWithSubscribes.foreach(_.start())

    val consumerWithSubscribes = manager.createConsumer("s2", List(0, 2), chooseOffsetPolicy(regularInstanceMetadata.startFrom), blockingQueue)
    consumerWithSubscribes.start()
    checkpointGroup.add(consumerWithSubscribes.name, consumerWithSubscribes)

//        new Thread(new Runnable {
//          def run() = {
//            val producer = manager.createProducer("s2")
//            val s = System.nanoTime
//            (0 until 10000) foreach { x =>
//              val txn = producer.newTransaction(ProducerPolicies.errorIfOpen)
//              (0 until 100) foreach { _ =>
//                txn.send(Array[Byte]())
//              }
//              txn.checkpoint()
//            }
//            println(s"producer time: ${(System.nanoTime - s) / 1000000}")
//          }
//        }).run()

    val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] =
      regularInstanceMetadata.outputs
        .map(x => (x, manager.createProducer(x))).toMap

    producers.foreach(x => checkpointGroup.add(x._2.name, x._2))

    val classLoader = manager.getClassLoader(moduleJar.getAbsolutePath)

    runModule(moduleTimer,
      regularInstanceMetadata,
      blockingQueue,
      temporaryOutput,
      classLoader,
      specification.executorClass,
      producers,
      serializer,
      manager,
      checkpointGroup)
  }

  private def sendData(output: (String, (String, Any)), producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]]) {
    output._2._1 match {
      case "partitioned" =>
        val partitionedData = output._2._2.asInstanceOf[mutable.Map[Int, mutable.MutableList[Array[Byte]]]]
        partitionedData.foreach(y => {
          val transaction = producers(output._1).newTransaction(ProducerPolicies.errorIfOpen, y._1)
          y._2.foreach(transaction.send)
          transaction.checkpoint()
          println(s"stream: ${output._1}, number of elements: ${y._2.length}")
        })
      case "round-robin" =>
        val transaction = producers(output._1).newTransaction(ProducerPolicies.errorIfOpen)
        val data = output._2._2.asInstanceOf[mutable.MutableList[Array[Byte]]]
        data.foreach(transaction.send)
        transaction.checkpoint()
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
                        manager: TaskEnvironmentManager,
                        checkpointGroup: CheckpointGroup) = {
    regularInstanceMetadata.stateManagement match {
      case "none" =>
        val moduleEnvironmentManager = new ModuleEnvironmentManager(
          regularInstanceMetadata.options,
          temporaryOutput,
          moduleTimer
        )

        val executor = classLoader.loadClass(pathToExecutor)
          .getConstructor(classOf[ModuleEnvironmentManager])
          .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

        executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
            while (true) {

              val transaction = serializer.deserialize[Transaction](blockingQueue.get())

              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              if (checkpointTimer.isTime) {
                checkpointGroup.commit()
                executor.onCheckpoint()
                checkpointTimer.reset()
                checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.reset()
              }
            }
          case "every-nth" =>
            var countOfTransaction = 0
            while (true) {

              val transaction = serializer.deserialize[Transaction](blockingQueue.get())
              countOfTransaction += 1

              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              if (countOfTransaction == regularInstanceMetadata.checkpointInterval) {
                checkpointGroup.commit()
                executor.onCheckpoint()
                countOfTransaction = 0
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.reset()
              }
            }
        }

      case "ram" =>
        var countOfCheckpoints = 0
        val taskName = System.getenv("TASK_NAME")
        val stateProducer = manager.createStateProducer(taskName)
        val stateConsumer = manager.createStateConsumer(taskName, Oldest)

        checkpointGroup.add(stateConsumer.name, stateConsumer)
        checkpointGroup.add(stateProducer.name, stateProducer)

        val stateService = new RAMStateService(stateProducer, stateConsumer)

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

        //executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
            var i = 0
            val s = System.nanoTime
            while (i < 1000) {

              val transaction = serializer.deserialize[Transaction](blockingQueue.get())

              i += 1
              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              if (checkpointTimer.isTime) {
                if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                  stateService.checkpoint()
                  countOfCheckpoints += 1
                } else {
                  stateService.fullCheckpoint()
                  countOfCheckpoints = 0
                }
                checkpointGroup.commit()
                executor.onCheckpoint()
                checkpointTimer.reset()
                checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.reset()
              }
            }
            val txnCount = stateService.get("txnCount").asInstanceOf[Int]
            println(s"count of txn = $txnCount")
            val elementCount = stateService.get("elementCount").asInstanceOf[Int]
            println(s"count of element = $elementCount")
            println(s"consumer time: ${(System.nanoTime - s) / 1000000}. Count = $i")
          case "every-nth" =>
            var countOfTransaction = 0
            while (true) {

              val transaction = serializer.deserialize[Transaction](blockingQueue.get())
              countOfTransaction += 1

              executor.run(transaction)

              if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
              temporaryOutput.clear()

              if (countOfTransaction == regularInstanceMetadata.checkpointInterval) {
                if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                  stateService.checkpoint()
                  countOfCheckpoints += 1
                } else {
                  stateService.fullCheckpoint()
                  countOfCheckpoints = 0
                }
                checkpointGroup.commit()
                executor.onCheckpoint()
                countOfTransaction = 0
              }

              if (moduleTimer.isTime) {
                executor.onTimer()
                moduleTimer.reset()
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
    manager.getState.set("txnCount", 0)
    manager.getState.set("elementCount", 0)
    println("new init")
  }

  override def finish(): Unit = ???

  override def onCheckpoint(): Unit = {
    println("onCheckpoint")
  }

  override def run(transaction: Transaction): Unit = {
 //   val output = manager.getRoundRobinOutput("s3")
    val state = manager.getState
    var elementCount = state.get("elementCount").asInstanceOf[Int]
    var txnCount = state.get("txnCount").asInstanceOf[Int]
    elementCount += transaction.data.length
    txnCount += 1
    state.set("txnCount", txnCount)
    state.set("elementCount", elementCount)
  }

  override def onTimer(): Unit = {
    println("onTimer")
  }
}
