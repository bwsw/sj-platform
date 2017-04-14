package com.bwsw.sj.module.input.regex

import java.util.regex.Pattern

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.{KafkaSjStream, SjStream, TStreamSjStream}
import com.bwsw.sj.common.utils.stream_distributor.{ByHash, SjStreamDistributor}
import com.bwsw.sj.common.utils.{AvroUtils, StreamLiterals}
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.utils.SeparateTokenizer
import com.bwsw.sj.engine.core.input.{InputStreamingExecutor, Interval}
import io.netty.buffer.ByteBuf
import org.apache.avro.SchemaBuilder.FieldAssembler
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.{Schema, SchemaBuilder}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Implementation of Input Streaming Executor for Regex Input Module
  *
  * @param manager Instance of InputEnvironmentManager used for receiving module's options
  * @author Ruslan Komarov
  */
class RegexInputExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Record](manager) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val serializer = new JsonSerializer
  private val regexInputOptions = serializer.deserialize[RegexInputOptions](manager.options)

  private val outputSchemas = regexInputOptions.rules.map(r => r -> createOutputSchema(r.fields)).toMap
  private val outputDistributors = regexInputOptions.rules.map(r => r -> createOutputDistributor(r)).toMap

  private val fallbackSchema = SchemaBuilder.record(RegexInputOptionsNames.fallbackRecordName).fields()
    .name(RegexInputOptionsNames.fallbackFieldName).`type`().stringType().noDefault().endRecord()

  private val fallbackPartitionCount = getPartitionCount(manager.outputs.find(_.name == regexInputOptions.fallbackStream).get)
  private val fallbackDistributor = new SjStreamDistributor(fallbackPartitionCount)

  private val tokenizer = new SeparateTokenizer(regexInputOptions.lineSeparator, regexInputOptions.encoding)

  private val policyHandler: String => Option[InputEnvelope[Record]] = regexInputOptions.policy match {
    case RegexInputOptionsNames.checkEveryPolicy => handleDataWithCheckEveryPolicy
    case RegexInputOptionsNames.firstMatchWinPolicy => handleDataWithFirstMatchWinPolicy
    case _ => throw new IllegalArgumentException(s"Incorrect or unsupported policy: ${regexInputOptions.policy}")
  }

  logger.info(s"Started with ${regexInputOptions.policy} policy")

  /**
    * Tokenize method implementation (uses SeparateTokenizer)
    *
    * @param buffer received data
    * @return Interval of ByteBuf that contains data (in bytes)
    */
  override def tokenize(buffer: ByteBuf): Option[Interval] = tokenizer.tokenize(buffer)

  /**
    * Parse method implementation. Receive data from tokenize method, parse it and pass on the output stream
    *
    * @param buffer   received data
    * @param interval Interval of buffer received from tokenize method
    * @return Option of InputEnvelop with data converted to Avro record
    */
  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Record]] = {
    val length = interval.finalValue - interval.initialValue
    val dataBuffer = buffer.slice(interval.initialValue, length)
    val data = new Array[Byte](length)

    dataBuffer.getBytes(0, data)
    buffer.readerIndex(interval.finalValue + 1)

    val line = Source.fromBytes(data, regexInputOptions.encoding).mkString

    logger.info(s"Received data $line")

    policyHandler(line)
  }

  private def handleDataWithCheckEveryPolicy(data: String): Option[InputEnvelope[Record]] = {
    // TODO: Change behavior for check-every policy
    logger.warn(s"${regexInputOptions.policy} policy is not implemented yet")
    buildFallbackEnvelope(data)
  }

  private def handleDataWithFirstMatchWinPolicy(data: String): Option[InputEnvelope[Record]] = {
    @tailrec
    def handleByRules(rulesList: List[Rule]): Option[InputEnvelope[Record]] = {
      rulesList match {
        case Nil =>
          logger.debug(s"Data $data was not match with all regex in rules list")
          buildFallbackEnvelope(data)

        case r :: rs if data.matches(r.regex) =>
          logger.debug(s"Data $data matched with regex ${r.regex}")
          buildOutputEnvelope(data, r)

        case r :: rs =>
          logger.debug(s"Data $data was not match with regex ${r.regex}")
          handleByRules(rs)
      }
    }

    handleByRules(regexInputOptions.rules)
  }

  private def buildOutputEnvelope(data: String, rule: Rule) = {
    logger.debug(s"Create input envelope: convert received data $data to Avro format using rule: $rule")

    val uniqueKey =
      if (rule.uniqueKey.nonEmpty) rule.uniqueKey
      else rule.fields.map(_.name)

    val ruleMatcher = Pattern.compile(rule.regex).matcher(data)
    val record = new Record(outputSchemas(rule))

    // Used to find the match in the data using the regex pattern
    if (ruleMatcher.find()) {
      rule.fields.foreach { field =>
        val fieldValue = Try[String](ruleMatcher.group(field.name)) match {
          case Success(value) => value
          case Failure(_) => field.defaultValue
        }
        record.put(field.name, fieldValue)
      }
    }

    logger.debug(s"Created Avro record from data: $record")

    val key = AvroUtils.concatFields(uniqueKey, record)

    Some(new InputEnvelope(
      s"${rule.outputStream}$key",
      Array((rule.outputStream, outputDistributors(rule).getNextPartition(record))),
      true,
      record))
  }

  private def buildFallbackEnvelope(data: String): Option[InputEnvelope[Record]] = {
    logger.debug(s"Create input envelope for fallback stream from data: $data")
    val record = new Record(fallbackSchema)
    record.put(RegexInputOptionsNames.fallbackFieldName, data)

    Some(new InputEnvelope(
      s"${regexInputOptions.fallbackStream},$data",
      Array((regexInputOptions.fallbackStream, fallbackDistributor.getNextPartition())),
      false,
      record))
  }

  private def getPartitionCount(sjStream: SjStream) = {
    sjStream match {
      case s: TStreamSjStream => s.partitions
      case s: KafkaSjStream => s.partitions
      case _ => throw new IllegalArgumentException(s"stream type must be ${StreamLiterals.tstreamType} or " +
        s"${StreamLiterals.kafkaStreamType}")
    }
  }

  private def createOutputSchema(fieldList: List[Field]) = {
    @tailrec
    def createSchemaInner(fieldList: List[Field], scheme: FieldAssembler[Schema]): Schema = {
      fieldList match {
        case Nil => scheme.endRecord()
        case f :: fs => createSchemaInner(fs, scheme.name(f.name).`type`().stringType().stringDefault(f.defaultValue))
      }
    }

    createSchemaInner(fieldList, SchemaBuilder.record(RegexInputOptionsNames.outputRecordName).fields())
  }

  private def createOutputDistributor(rule: Rule) = {
    val outputPartitionCount = getPartitionCount(manager.outputs.find(_.name == rule.outputStream).get)

    if (rule.distribution.isEmpty) new SjStreamDistributor(outputPartitionCount)
    else new SjStreamDistributor(outputPartitionCount, ByHash, rule.distribution)
  }
}