package com.bwsw.sj.common.module.environment

import scala.collection.mutable

/**
 * Provides an output stream that defined for each partition
 * Created: 20/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param output Output of stream
 */

class PartitionedOutput(output: mutable.Map[Int, mutable.MutableList[Array[Byte]]]) {
  def put(data: Array[Byte], partition: Int) =
    if (output.contains(partition)) output(partition).+=(data)
    else output(partition) = mutable.MutableList[Array[Byte]](data)
}
