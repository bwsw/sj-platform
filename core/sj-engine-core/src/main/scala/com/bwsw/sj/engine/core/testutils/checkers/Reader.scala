package com.bwsw.sj.engine.core.testutils.checkers

/**
  * Returns a data from stream
  *
  * @author Pavel Tomskikh
  */
trait Reader[+T] {

  /**
    * Returns a data from stream
    *
    * @return a data from stream
    */
  def get(): Seq[T]
}
