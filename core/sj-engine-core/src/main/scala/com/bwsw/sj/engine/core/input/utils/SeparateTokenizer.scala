package com.bwsw.sj.engine.core.input.utils

import com.bwsw.sj.engine.core.input.Interval
import io.netty.buffer.ByteBuf
import io.netty.util.ByteProcessor

/**
  * Tokenize buffer by separator string.
  *
  * @author Pavel Tomskikh
  */
class SeparateTokenizer(separator: String) {

  private val byteProcessor = new ByteProcessor {
    private var read: String = ""

    override def process(value: Byte) = {
      read += value
      !read.endsWith(separator)
    }
  }

  def tokenize(buffer: ByteBuf): Option[Interval] = {
    val startIndex = buffer.readerIndex()
    val writerIndex = buffer.writerIndex()


    val endIndex = buffer.forEachByte(startIndex, writerIndex, byteProcessor)

    if (endIndex != -1) Some(Interval(startIndex, endIndex))
    else None
  }
}
