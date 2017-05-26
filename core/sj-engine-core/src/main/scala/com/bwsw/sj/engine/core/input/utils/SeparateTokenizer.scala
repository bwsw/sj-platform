package com.bwsw.sj.engine.core.input.utils

import com.bwsw.sj.engine.core.input.Interval
import io.netty.buffer.ByteBuf
import io.netty.util.ByteProcessor

import scala.io.Source

/**
  * Tokenize buffer by separator.
  * It collects bytes while the separator doesn't appear.
  *
  * @param separator a byte separator
  * @param encoding  encoding type of message data
  * @author Pavel Tomskikh
  */
class SeparateTokenizer(separator: String, encoding: String) {

  private val byteProcessor = new ByteProcessor {
    private var bytes: Array[Byte] = Array.empty

    override def process(value: Byte): Boolean = {
      bytes = bytes :+ value
      val line = Source.fromBytes(bytes, encoding).mkString
      !line.endsWith(separator)
    }
  }

  def tokenize(buffer: ByteBuf): Option[Interval] = {
    val startIndex = buffer.readerIndex()
    val endIndex = buffer.forEachByte(byteProcessor)

    if (endIndex != -1) Some(Interval(startIndex, endIndex))
    else None
  }
}
