package com.bwsw.sj.engine.windowed

import com.bwsw.sj.engine.core.entities.Envelope

trait EnvelopeDataDeserializer[T <: Envelope] {
  def deserialize(envelope: Envelope): T
}

trait TypedDataEnvelope {

}

trait BatchCollectorT[T <: Envelope] {
  def onReceive(envelope: T): Unit
}

class MyTypedDataEnvelope extends Envelope

class a extends EnvelopeDataDeserializer[MyTypedDataEnvelope] {
  override def deserialize(envelope: Envelope) = {
    new MyTypedDataEnvelope
  }
}

class b extends BatchCollectorT[MyTypedDataEnvelope] {
  override def onReceive(envelope: MyTypedDataEnvelope): Unit = {
    println(envelope.getClass)
  }
}

object a {
  def main(args: Array[String]): Unit = {
    new b().onReceive({
      new a().deserialize(new Envelope)
    })
  }
}