package com.bwsw.sj.common.module.entities

import java.util.UUID

/**
 * Class-wrapper for t-stream transaction
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

case class Transaction(stream: String, partition: Int, txnUUID: UUID, consumerID: String, data: List[Array[Byte]])

