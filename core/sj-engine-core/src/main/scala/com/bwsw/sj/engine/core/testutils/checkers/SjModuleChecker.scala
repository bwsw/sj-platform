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
package com.bwsw.sj.engine.core.testutils.checkers

import com.bwsw.sj.common.utils.benchmark.ProcessTerminator
import com.bwsw.tstreams.agents.consumer.Consumer

/**
  * Validates that data in output stream corresponds data in output stream and data in state is correct
  *
  * @param inputElementsReaders reads data from input stream
  * @param outputElementsReader reads data from output stream
  * @param stateConsumer        consumer for state stream
  * @author Pavel Tomskikh
  */
abstract class SjModuleChecker[T](inputElementsReaders: Seq[Reader[T]],
                                  outputElementsReader: Reader[T],
                                  stateConsumer: Option[Consumer] = None) extends App {
  ProcessTerminator.terminateProcessAfter { () =>
    val inputElements = inputElementsReaders.flatMap(_.get())
    val outputElements = outputElementsReader.get()

    val maybeSum = stateConsumer.map(StateReader.getStateSum)

    val (missed, extra) = difference(inputElements, outputElements)

    assert(extra.isEmpty && missed.isEmpty,
      "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream. " +
        s"Extra elements in output stream: ${extra.mkString(", ")}. " +
        s"Missed elements in output stream: ${missed.mkString(", ")}.")

    maybeSum.foreach(sum =>
      assert(sum == inputElements.map(_.asInstanceOf[Int]).sum,
        "Sum of all txns elements that are consumed from input stream should equals state variable sum"))
  }

  /**
    * Returns a difference between two collections
    *
    * @return
    */
  def difference[Y](xs: Seq[Y], ys: Seq[Y]): (Seq[Y], Seq[Y]) = {
    xs.foldLeft((Seq.empty[Y], ys.toBuffer))({
      case ((missedInYs, extraInYs), element) =>
        if (extraInYs.contains(element)) (missedInYs, extraInYs - element)
        else (missedInYs :+ element, extraInYs)
    })
  }
}
