package com.bwsw.sj.engine.core.utils

import java.util.Date

import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.DAL.model.{TStreamSjStream, ZKService}
import com.bwsw.sj.common.utils.ConfigUtils
import com.bwsw.tstreams.agents.consumer.Offsets.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.generator.{IUUIDGenerator, LocalTimeUUIDGenerator}

/**
  * Created: 27/05/2016
  *
  * @author Kseniya Tomskikh
  */
object EngineUtils {

  private val retryPeriod = ConfigUtils.getClientRetryPeriod()
  private val retryCount = ConfigUtils.getRetryCount()

  /**
    * Chooses offset policy for t-streams consumers
    *
    * @param startFrom Offset policy name or specific date
    * @return Offset
    */
  def chooseOffset(startFrom: String): IOffset = {
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }

  /**
    * Creating UUID generator for t-stream
    *
    * @param stream T-stream object
    * @return UUID generator
    */
  def getUUIDGenerator(stream: TStreamSjStream) : IUUIDGenerator = {
    stream.generator.generatorType match {
      case "local" => new LocalTimeUUIDGenerator
      case generatorType =>
        val service = stream.generator.service.asInstanceOf[ZKService]
        val zkHosts = service.provider.hosts
        val prefix = "/" + service.namespace + "/" + {
          if (generatorType == "global") generatorType else stream.name
        }

        new NetworkTimeUUIDGenerator(zkHosts, prefix, retryPeriod, retryCount)
    }
  }

  def portIsOpen(address:String, port:Int): Boolean = {
    var closed = true
    val socket = new java.net.Socket()
    try {
      socket.setReuseAddress(true)
      socket.connect(new java.net.InetSocketAddress(address, port), 5)
      socket.close()
      closed = false
    } catch {
      case e:Exception =>
    }
    !closed
  }

}
