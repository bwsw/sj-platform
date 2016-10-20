package com.bwsw.sj.engine.core.entities

case class Transaction(partition: Int, id: Long, data: List[Array[Byte]])
