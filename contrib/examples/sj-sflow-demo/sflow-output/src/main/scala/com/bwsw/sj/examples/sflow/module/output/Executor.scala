package com.bwsw.sj.examples.sflow.module.output

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.examples.sflow.module.output.data._

/**
  * Created by diryavkin_dn on 13.01.17.
  */
class Executor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[Array[Byte], String](manager) {
  val jsonSerializer = new JsonSerializer()
  val objectSerializer = new ObjectSerializer()

  /**
    * Transform t-stream transaction to output entities
    *
    * @param envelope Input T-Stream envelope
    * @return List of output envelopes
    */
  override def onMessage(envelope: TStreamEnvelope[Array[Byte]]): List[Envelope] = {
    val list:List[Envelope] = List[Envelope]()
    envelope.data.foreach { row =>
      val value = objectSerializer.deserialize(row)
      envelope.stream match {
        case "SrcAsStream" =>
          val defValue = value.asInstanceOf[collection.mutable.Map[Int, Int]]
          list ::: defValue.map(pair => new SrcAsData(pair._1, pair._2)).toList
        case "DstAsStream" =>
          val defValue = value.asInstanceOf[collection.mutable.Map[Int, Int]]
          list ::: defValue.map(pair => new DstAsData(pair._1, pair._2)).toList
        case "SrcDstStream" =>
          val defValue = value.asInstanceOf[collection.mutable.Map[Int Tuple2 Int, Int]]
          list ::: defValue.map(pair => new SrcDstData(pair._1._1, pair._1._2, pair._2)).toList
        case "SrcIpStream" =>
          val defValue = value.asInstanceOf[collection.mutable.Map[String, Int]]
          list ::: defValue.map(pair => new SrcIpData(pair._1, pair._2)).toList
        case "DstIpStream" =>
          val defValue = value.asInstanceOf[collection.mutable.Map[String, Int]]
          list ::: defValue.map(pair => new DstIpData(pair._1, pair._2)).toList
      }
    }
    list
  }

  override def getOutputModule = {
    entityBuilder
      .build()
  }
}
