package com.bwsw.sj.common.si.result

trait DeletionResult

case object Deleted extends DeletionResult

trait NotDeleted extends DeletionResult

case class DeletionError(error: String = "") extends NotDeleted

case object EntityNotFound extends NotDeleted
