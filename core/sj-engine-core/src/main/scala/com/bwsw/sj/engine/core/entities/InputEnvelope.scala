package com.bwsw.sj.engine.core.entities

/**
 * Provides a wrapper for t-stream transactions (if outputMetadata has more than one element) that is formed by input engine.
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

abstract class InputEnvelope(var key: String,
                             var outputMetadata: Array[(String, Int)],
                             var duplicateCheck: Boolean,
                             var data: Array[Byte]) {
}