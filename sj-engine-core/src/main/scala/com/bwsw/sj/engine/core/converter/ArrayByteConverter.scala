package com.bwsw.sj.engine.core.converter

import com.bwsw.tstreams.converter.IConverter

/**
  * Created: 31/05/2016
  *
  * @author Kseniya Tomskikh
  */
class ArrayByteConverter extends IConverter[Array[Byte], Array[Byte]] {
  override def convert(obj: Array[Byte]): Array[Byte] = {
    obj
  }
}
