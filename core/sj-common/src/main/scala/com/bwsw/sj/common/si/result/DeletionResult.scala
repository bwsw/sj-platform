package com.bwsw.sj.common.si.result

import com.bwsw.sj.common.si.model.instance.Instance

trait DeletionResult

case object Deleted extends DeletionResult

trait NotDeleted extends DeletionResult

case class DeletionError(error: String = "") extends NotDeleted

case object EntityNotFound extends NotDeleted

case class WillBeDeleted(instance: Instance) extends DeletionResult
