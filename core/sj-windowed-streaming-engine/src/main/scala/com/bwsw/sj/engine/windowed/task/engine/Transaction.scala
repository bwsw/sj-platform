package com.bwsw.sj.engine.windowed.task.engine

case class Transaction(partition: Int, id: Long,data: List[Array[Byte]])
