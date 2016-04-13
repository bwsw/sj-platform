package com.bwsw.sj.common.module.regular

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.module.{ModuleEnvironmentManager, TaskEnvironmentManager}
import com.bwsw.sj.common.module.entities.{Transaction, TaskParameters}

import scala.collection.mutable

/**
 * Object responsible for running a task of job
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner extends App {

  val serializer = new JsonSerializer()
  val taskParameters = serializer.deserialize[TaskParameters](args(0))

  val taskEnvironmentManager = new TaskEnvironmentManager()
  val temporaryOutput = mutable.Map(taskParameters.outputs.map(x => (x, mutable.MutableList[Array[Byte]]())): _*)
  val moduleEnvironmentManager = new ModuleEnvironmentManager(
    Map(),
    taskEnvironmentManager.getStateStorage(taskParameters.stateStorage),
    taskParameters.outputs,
    temporaryOutput
  )

  val consumers = taskParameters.inputsWithPartitions.map(x => taskEnvironmentManager.createConsumer(x._1, x._2)).toVector
  val producers = taskParameters.outputs.map(x => (x, taskEnvironmentManager.createProducer(x))).toMap

  val classLoader = taskEnvironmentManager.getClassLoader(taskParameters.pathToJar)
  val executor = classLoader.loadClass(taskParameters.pathToExecutor)
    .getConstructor(classOf[ModuleEnvironmentManager])
    .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

  executor.init()

  //если бы было можно подписаться на consumer, то по появлению новой транзакции - выполнялось следующее:
  while (true) {
    consumers.foreach(x => {
      val maybeTransaction = x.getTransaction
      if (maybeTransaction.isDefined) {
        executor.run(Transaction(x.stream.getName, maybeTransaction.get.getAll()))
        temporaryOutput.filter(x => x._2.nonEmpty).foreach(x => {
          /*
          *
          * 1) создать транзакцию
          * 2) отправить массив байт
          * 3) GroupCheckpoint или по времени (зависит от настройки)
          * 4) executor.onCheckpoint()
          * 5) обнуляем temporaryOutput (вторую составляющую)
          *
          * */
        })
      }
    })
  }

}
