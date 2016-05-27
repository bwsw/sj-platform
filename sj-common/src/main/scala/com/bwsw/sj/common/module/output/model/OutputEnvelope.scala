package com.bwsw.sj.common.module.output.model

import java.util.UUID

import com.bwsw.sj.common.module.entities.Envelope

/**
  * Created: 26/05/2016
  *
  * @author Kseniya Tomskikh
  */
class OutputEnvelope extends Envelope {
  var txn: UUID = null
  var data: OutputEntity = null
}
