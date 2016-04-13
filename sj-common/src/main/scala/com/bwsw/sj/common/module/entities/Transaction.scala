package com.bwsw.sj.common.module.entities

/**
 * Class-wrapper for t-stream transaction
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

case class Transaction(stream: String, data: List[Array[Byte]])
