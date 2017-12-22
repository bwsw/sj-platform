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

import com.bwsw.sj.common.engine.core.input.Interval
import io.netty.buffer.Unpooled
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for [[Tokenizer]]
  *
  * @author Pavel Tomskikh
  */
class TokenizerTests extends FlatSpec with Matchers with TableDrivenPropertyChecks {

  val utf8 = "UTF-8"

  "Tokenizer" should "tokenize interval if buffer contains separator" in {
    val separator = ".."
    val table = Table(("inputString", "interval"),
      ("test..test..test..test", Interval(0, 5)),
      ("test..test..test..test", Interval(3, 5)),
      ("test..test..test..test", Interval(4, 5)),
      ("test..test..test..test", Interval(5, 11)),
      ("test..test..test..test", Interval(15, 17)))

    forAll(table) { (inputString, interval) =>
      val buffer = Unpooled.buffer()
        .writeBytes(inputString.getBytes(utf8))
        .readerIndex(interval.initialValue)
      val tokenizer = new Tokenizer(separator, utf8)

      tokenizer.tokenize(buffer) shouldBe Some(interval)
    }
  }

  it should "not tokenize if buffer does not contains separator" in {
    val separator = ".."
    val table = Table(("inputString", "readerIndex"),
      ("test,test,test,test", 0),
      ("test,test,test,test", 3),
      ("test,test,test,test", 4),
      ("test,test,test,test", 5),
      ("test,test,test,test", 15))

    forAll(table) { (inputString, readerIndex) =>
      val buffer = Unpooled.buffer()
        .writeBytes(inputString.getBytes(utf8))
        .readerIndex(readerIndex)
      val tokenizer = new Tokenizer(separator, utf8)

      tokenizer.tokenize(buffer) shouldBe empty
    }
  }
}