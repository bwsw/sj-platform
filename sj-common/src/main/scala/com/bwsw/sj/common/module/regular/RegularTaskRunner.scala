package com.bwsw.sj.common.module.regular

import java.io.{BufferedReader, File, InputStreamReader}
import java.net.{URL, URLClassLoader}
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit._

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.{RegularInstanceMetadata, Specification}
import com.bwsw.sj.common.module.entities.Transaction
import com.bwsw.sj.common.module.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.common.module.state.DefaultModuleStateStorage
import com.bwsw.sj.common.module.{SjTimer, TaskEnvironmentManager}
import com.bwsw.tstreams.agents.consumer.BasicConsumer
import com.bwsw.tstreams.agents.consumer.Offsets.{IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}
import org.apache.commons.io.FileUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

import scala.collection.mutable

/**
 * Object responsible for running a task of job
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner {

  def chooseOffsetPolicy(startFrom: String): IOffset = {
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      //todo add two cases for date and UUID
    }
  }

  def main(args: Array[String]) {
    val serializer = new JsonSerializer()
    val moduleType = System.getenv("MODULE_TYPE")
    val moduleName = System.getenv("MODULE_NAME")
    val moduleVersion = System.getenv("MODULE_VERSION")
    val instanceName = System.getenv("INSTANCE_NAME")
    val taskName = System.getenv("TASK_NAME")
    val moduleJar = new File(s"$moduleName.jar")

    //todo replace this stub
    val crudRestAddress = "192.168.1.180:8887"

    FileUtils.copyURLToFile(new URL(s"http://$crudRestAddress/v1/modules/$moduleType/$moduleName/$moduleVersion"), moduleJar)

    val regularInstanceMetadata =
      serializer.deserialize[RegularInstanceMetadata](sendHttpGetRequest(s"http://$crudRestAddress/v1/modules/$moduleType/$moduleName/$moduleVersion/instance/$instanceName"))

    val specification =
      serializer.deserialize[Specification](sendHttpGetRequest(s"http://$crudRestAddress/v1/modules/$moduleType/$moduleName/$moduleVersion/specification"))

    val taskEnvironmentManager = new TaskEnvironmentManager()

    val temporaryOutput = mutable.Map(regularInstanceMetadata.outputs.map(x => (x, mutable.MutableList[Array[Byte]]())): _*)
    val moduleTimer = new SjTimer()

    val consumers = regularInstanceMetadata.executionPlan.tasks(taskName).inputs
      .map(x => taskEnvironmentManager.createConsumer(x._1, x._2, chooseOffsetPolicy(regularInstanceMetadata.startFrom))).toVector
    val producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]] = regularInstanceMetadata.outputs
      .map(x => (x, taskEnvironmentManager.createProducer(x))).toMap

    val classLoader = taskEnvironmentManager.getClassLoader(moduleJar.getAbsolutePath)

    //todo: stub for publish subscribe
    val transactionQueue: ArrayBlockingQueue[Transaction] = new ArrayBlockingQueue[Transaction](1000, true)

    new Thread(new Runnable {
      def run() {
        var i = 0
        while (i < 5) {
          Thread.sleep(1000)
          transactionQueue.add(Transaction("s1", 0, UUID.randomUUID(), "test_consumer",
            List("wow!".getBytes,
              "wow!!".getBytes,
              "wow!!!".getBytes)
          ))
          i = i + 1
        }
      }
    }).start()

    runModule(moduleTimer, regularInstanceMetadata, transactionQueue, temporaryOutput, classLoader, specification.executorClass, producers)
  }

  private def runModule(moduleTimer: SjTimer,
                        regularInstanceMetadata: RegularInstanceMetadata,
                        transactionQueue: ArrayBlockingQueue[Transaction],
                        temporaryOutput: mutable.Map[String, mutable.MutableList[Array[Byte]]],
                        classLoader: URLClassLoader,
                        pathToExecutor: String,
                        producers: Map[String, BasicProducer[Array[Byte], Array[Byte]]]) = {
    regularInstanceMetadata.stateManagement match {
      case "none" =>
        val moduleEnvironmentManager = new ModuleEnvironmentManager(
          regularInstanceMetadata.options,
          regularInstanceMetadata.outputs,
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
              val transaction: Transaction = transactionQueue.poll(2000, MILLISECONDS)
              if (transaction != null) {
                executor.run(transaction)
                temporaryOutput.filter(_._2.nonEmpty).foreach(x => {
                  val transaction: BasicProducerTransaction[Array[Byte], Array[Byte]] = producers(x._1).newTransaction(ProducerPolicies.errorIfOpen)
                  x._2.foreach(transaction.send)
                  transaction.close()
                  println(s"stream: ${x._1}, number of elements: ${x._2.length}")
                })
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
              //todo replace rest-api address
              val transaction: Transaction = transactionQueue.poll(2000, MILLISECONDS)
              if (transaction != null) {
                countOfTransaction += 1
                executor.run(transaction)
                temporaryOutput.filter(_._2.nonEmpty).foreach(x => {
                  val transaction = producers(x._1).newTransaction(ProducerPolicies.errorIfOpen)
                  x._2.foreach(transaction.send)
                  transaction.close()
                  println(s"stream: ${x._1}, number of elements: ${x._2.length}")
                })
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
          //todo producer & consumer for state stream
          new BasicProducer[Array[Byte], Array[Byte]]("stub producer", null, null),
          new BasicConsumer[Array[Byte], Array[Byte]]("stub consumer", null, null))

        val moduleEnvironmentManager = new StatefulModuleEnvironmentManager(
          moduleStateStorage,
          regularInstanceMetadata.options,
          regularInstanceMetadata.outputs,
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
            checkpointTimer.setTimer(regularInstanceMetadata.checkpointInterval)
            while (true) {
              //todo replace rest-api address
              val transaction: Transaction = transactionQueue.poll(2000, MILLISECONDS)
              if (transaction != null) {
                executor.run(transaction)
                temporaryOutput.filter(_._2.nonEmpty).foreach(x => {
                  val transaction = producers(x._1).newTransaction(ProducerPolicies.errorIfOpen)
                  x._2.foreach(transaction.send)
                  transaction.close()
                  println(s"stream: ${x._1}, number of elements: ${x._2.length}")
                })
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
              //todo replace rest-api address
              val transaction: Transaction = transactionQueue.poll(2000, MILLISECONDS)
              if (transaction != null) {
                countOfTransaction += 1
                executor.run(transaction)
                temporaryOutput.filter(_._2.nonEmpty).foreach(x => {
                  val transaction = producers(x._1).newTransaction(ProducerPolicies.errorIfOpen)
                  x._2.foreach(transaction.send)
                  transaction.close()
                  println(s"stream: ${x._1}, number of elements: ${x._2.length}")
                })
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
    }

  }

  def sendHttpGetRequest(url: String) = {

    val client = HttpClientBuilder.create().build()
    val request = new HttpGet(url)

    val response = client.execute(request)

    System.out.println("Response Code : "
      + response.getStatusLine.getStatusCode)

    val rd = new BufferedReader(
      new InputStreamReader(response.getEntity.getContent))

    val result = new StringBuffer()
    var line: String = rd.readLine()
    while (line != null) {
      result.append(line)
      line = rd.readLine()
    }

    result.toString
  }
}