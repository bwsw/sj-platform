/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.engine.core.input.utils

import java.nio.charset.Charset

import com.bwsw.sj.common.engine.core.input.Interval
import io.netty.buffer.ByteBuf
import io.netty.util.ByteProcessor

import scala.io.Source

/**
  * Splits buffer by separator.
  * It collects bytes while the separator doesn't appear.
  *
  * @param separator the string used as a delimiter
  * @param encoding  name of encoding
  * @author Pavel Tomskikh
  */
class Tokenizer(separator: String, encoding: String) {
  require(separator.length > 0, "separator must be nonempty")
  require(Charset.isSupported(encoding), s"encoding '$encoding' does not supported")

  private class TokenizerByteProcessor extends ByteProcessor {
    private var bytes: Array[Byte] = Array.emptyByteArray

    override def process(value: Byte): Boolean = {
      bytes :+= value
      val line = new String(bytes, encoding)

      !line.endsWith(separator)
    }

    def clear(): Unit =
      bytes = Array.emptyByteArray
  }

  private val byteProcessor = new TokenizerByteProcessor

  /**
    * Splits buffer by separator
    *
    * @param buffer buffer of bytes
    * @return corresponding [[Interval]] if separator is found or None otherwise
    */
  def tokenize(buffer: ByteBuf): Option[Interval] = {
    val startIndex = buffer.readerIndex()
    val endIndex = buffer.forEachByte(byteProcessor)
    byteProcessor.clear()

    if (endIndex != -1) Some(Interval(startIndex, endIndex))
    else None
  }
}
