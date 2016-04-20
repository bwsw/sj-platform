package com.bwsw.sj.common.module.entities

import java.util.UUID

/**
 * Provides a wrapper for t-stream transaction.
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva

 * @param stream Stream name from which a transaction received
 * @param partition Number of stream partition from which a transaction received
 * @param txnUUID Transaction UUID
 * @param consumerID Consumer extracted a transaction
 * @param data Transaction data
 */

case class Transaction(stream: String, partition: Int, txnUUID: UUID, consumerID: String, data: List[Array[Byte]])

