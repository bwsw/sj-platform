package com.bwsw.sj.common.module.regular

import java.net.URLClassLoader

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.RegularInstanceMetadata
import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.module.state.{RAMStateService, StateStorage}
import com.bwsw.sj.common.module.utils.SjTimer
import com.bwsw.sj.common.module.{PersistentBlockingQueue, TaskManager}
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

    val manager = new TaskManager()

    val moduleJar = manager.downloadModuleJar()

    val regularInstanceMetadata: RegularInstanceMetadata = manager.getRegularInstanceMetadata(serializer)

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

    val consumerWithSubscribes = manager.createConsumer("s2", List(0, 2), chooseOffset(regularInstanceMetadata.startFrom), blockingQueue)
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

  /**
   * Transmits an user-modified data from temporary output using t-streams producers
   * @param output Temporary output that available into module
   * @param producers T-streams producers for each output stream of instance parameters
   */
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

  /**
   * Provides an imitation of streaming processing using blocking queue
   * @param moduleTimer Provides a timer inside module
   * @param regularInstanceMetadata Launch parameters of module
   * @param blockingQueue Queue for keeping transaction
   * @param temporaryOutput Provides a wrapper for each output stream
   * @param classLoader Allows loading an executor class
   * @param pathToExecutor Absolute class path to module class that implemented RegularStreamingExecutor
   * @param producers T-streams producers for sending data to output streams
   * @param serializer Json serializer for deserialization of transaction
   * @param manager Allows managing an environment of task
   * @param checkpointGroup Group of producers and consumers which should do a checkpoint at the same time
   */
  private def runModule(moduleTimer: SjTimer,
                        regularInstanceMetadata: RegularInstanceMetadata,
                        blockingQueue: PersistentBlockingQueue,
                        temporaryOutput: mutable.Map[String, (String, Any)],
                        classLoader: URLClassLoader,
                        pathToExecutor: String,
                        producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]],
                        serializer: JsonSerializer,
                        manager: TaskManager,
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

              val maybeTxn = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeTxn == null) {
                executor.onIdle()
              } else {
                val transaction = serializer.deserialize[Transaction](maybeTxn)
                executor.onTxn(transaction)

                if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
                temporaryOutput.clear()

                if (checkpointTimer.isTime) {
                  executor.onBeforeCheckpoint()
                  checkpointGroup.commit()
                  executor.onAfterCheckpoint()
                  checkpointTimer.reset()
                  checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
                }

                if (moduleTimer.isTime) {
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
          case "every-nth" =>
            var countOfTransaction = 0
            while (true) {

              val maybeTxn = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeTxn == null) {
                executor.onIdle()
              } else {
                val transaction = serializer.deserialize[Transaction](maybeTxn)
                countOfTransaction += 1

                executor.onTxn(transaction)

                if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
                temporaryOutput.clear()

                if (countOfTransaction == regularInstanceMetadata.checkpointInterval) {
                  executor.onBeforeCheckpoint()
                  checkpointGroup.commit()
                  executor.onAfterCheckpoint()
                  countOfTransaction = 0
                }

                if (moduleTimer.isTime) {
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
        }

      case "ram" =>
        var countOfCheckpoints = 1
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

        executor.init()

        regularInstanceMetadata.checkpointMode match {
          case "time-interval" =>
            val checkpointTimer = new SjTimer()
            checkpointTimer.set(regularInstanceMetadata.checkpointInterval)

            while (true) {

              val maybeTxn = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeTxn == null) {
                executor.onIdle()
              } else {
                val transaction = serializer.deserialize[Transaction](maybeTxn)
                executor.onTxn(transaction)

                if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
                temporaryOutput.clear()

                if (checkpointTimer.isTime) {
                  if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                    executor.onBeforeStateSave(false)
                    stateService.checkpoint()
                    executor.onAfterStateSave(false)
                    countOfCheckpoints += 1
                  } else {
                    executor.onBeforeStateSave(true)
                    stateService.fullCheckpoint()
                    executor.onAfterStateSave(true)
                    countOfCheckpoints = 0
                  }

                  executor.onBeforeCheckpoint()
                  checkpointGroup.commit()
                  executor.onAfterCheckpoint()

                  checkpointTimer.reset()
                  checkpointTimer.set(regularInstanceMetadata.checkpointInterval)
                }

                if (moduleTimer.isTime) {
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }

          case "every-nth" =>
            var countOfTransaction = 0

            while (true) {

              val maybeTxn = blockingQueue.get(regularInstanceMetadata.idle)

              if (maybeTxn == null) {
                executor.onIdle()
              } else {
                val transaction = serializer.deserialize[Transaction](maybeTxn)
                countOfTransaction += 1

                executor.onTxn(transaction)

                if (temporaryOutput.nonEmpty) temporaryOutput.foreach(x => sendData(x, producers))
                temporaryOutput.clear()

                if (countOfTransaction == regularInstanceMetadata.checkpointInterval) {
                  if (countOfCheckpoints != regularInstanceMetadata.stateFullCheckpoint) {
                    stateService.checkpoint()
                    executor.onAfterStateSave(false)
                    countOfCheckpoints += 1
                  } else {
                    stateService.fullCheckpoint()
                    executor.onAfterStateSave(true)
                    countOfCheckpoints = 1
                  }

                  executor.onBeforeCheckpoint()
                  checkpointGroup.commit()
                  executor.onAfterCheckpoint()

                  countOfTransaction = 0
                }

                if (moduleTimer.isTime) {
                  executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
                  moduleTimer.reset()
                }
              }
            }
        }
    }

  }

  /**
   * Chooses offset policy for t-streams consumers
   * @param startFrom Offset policy name or specific date or transaction UUID
   * @return Offset
   */
  private def chooseOffset(startFrom: String): IOffset = {
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      //todo add two cases for date and UUID
    }
  }

}

class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

  val state = manager.getState

  override def init(): Unit = {
    println("new init")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onTxn(transaction: Transaction): Unit = {
    //   val output = manager.getRoundRobinOutput("s3")
    //var elementCount = state.get("elementCount").asInstanceOf[Int]
    //    var txnCount = state.get(transaction.txnUUID.toString).asInstanceOf[Int]
    //elementCount += transaction.data.length
    state.set(transaction.txnUUID.toString, transaction.data.length)
    //state.set("elementCount", elementCount)
  }

  override def onTimer(jitter: Long): Unit = {
    println("onTimer")
  }

  override def onAfterStateSave(isFull: Boolean): Unit = {
    if (isFull) state.clear()
  }

  override def onBeforeCheckpoint(): Unit = {
    println("on before checkpoint")
  }

  override def onIdle(): Unit = {
    println("on Idle")
  }

  /**
   * Handler triggered before save state
   * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  override def onBeforeStateSave(isFullState: Boolean): Unit = {
    println("on before state save")
  }
}
