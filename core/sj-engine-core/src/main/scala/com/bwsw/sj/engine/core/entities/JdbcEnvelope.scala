package com.bwsw.sj.engine.core.entities

/**
 * Represents a message that is received from an OutputExecutor
 *
 * Provides a wrapper for jdbc entity.
 */

class JdbcEnvelope extends Envelope with Serializable {
  streamType = "jdbc-output"
}
