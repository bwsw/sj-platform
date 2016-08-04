package com.bwsw.sj.engine.core.entities

case class EchoResponse(ts: Long, ip: String, time: Double)

case class UnreachableResponse(ts: Long, ip: String)