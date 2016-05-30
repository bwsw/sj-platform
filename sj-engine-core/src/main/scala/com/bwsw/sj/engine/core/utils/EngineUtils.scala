package com.bwsw.sj.engine.core.utils

import java.util.Date

import com.bwsw.tstreams.agents.consumer.Offsets.{DateTime, Newest, Oldest, IOffset}

/**
  * Created: 27/05/2016
  *
  * @author Kseniya Tomskikh
  */
object EngineUtils {

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

}
