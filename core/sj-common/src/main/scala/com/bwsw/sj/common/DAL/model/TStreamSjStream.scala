package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.stream.{GeneratorData, TStreamSjStreamData}
import org.mongodb.morphia.annotations.Embedded

class TStreamSjStream() extends SjStream {
  var partitions: Int = 0
  @Embedded var generator: Generator = null

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: Array[String],
           generator: Generator) = {
    this()
    this.name = name
    this.description = description
    this.partitions = partitions
    this.service = service
    this.streamType = streamType
    this.tags = tags
    this.generator = generator
  }

  override def asProtocolStream() = {
    val streamData = new TStreamSjStreamData
    super.fillProtocolStream(streamData)


    streamData.partitions = this.partitions
    streamData.generator = createGeneratorData()

    streamData
  }

  private def createGeneratorData() = {
    this.generator.generatorType match {
      case "local" => new GeneratorData(this.generator.generatorType)
      case _ => new GeneratorData(this.generator.generatorType, this.generator.service.name, this.generator.instanceCount)
    }
  }
}
