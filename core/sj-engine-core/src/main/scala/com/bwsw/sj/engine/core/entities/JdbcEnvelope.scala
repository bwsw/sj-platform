package com.bwsw.sj.engine.core.entities

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a message that is received from an OutputExecutor
 *
 * Provides a wrapper for jdbc entity.
 */

class JdbcEnvelope extends Envelope with Serializable {
  streamType = "jdbc-output"

  /**
    * Get field value by name.
    * @param name field name
    * @return
    */
  def getV(name: String): Any = this.getClass.getMethods.find(_.getName == name).get.invoke(this)

  /**
    * Set field value by name.
    * @param name field name
    * @param value field value
    */
  def setV(name: String, value: Any): Unit = this.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(this, value.asInstanceOf[AnyRef])

  /**
    * Unique identifier for stream transaction.
    */
  var txn: String = ""

  def getTxnName="txn"
}
