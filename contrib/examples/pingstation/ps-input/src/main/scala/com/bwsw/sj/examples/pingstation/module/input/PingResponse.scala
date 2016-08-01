package com.bwsw.sj.examples.pingstation.module.input

case class EchoResponse(ts: Long, ip: String, time: Double)

case class UnreachableResponse(ts: Long, ip: String)
