package com.bwsw.sj.engine.batch.module.checkers.elements_readers

/**
  * Reads elements from input stream
  *
  * @author Pavel Tomskikh
  */
trait InputElementsReader {

  /**
    * Reads elements from input stream
    *
    * @return elements from input stream
    */
  def getInputElements(): Seq[Int]
}
