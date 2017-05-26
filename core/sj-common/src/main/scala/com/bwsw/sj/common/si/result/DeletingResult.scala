package com.bwsw.sj.common.si.result

trait DeletingResult

case object Deleted extends DeletingResult

trait NotDeleted extends DeletingResult

case class DeletingError(error: String = "") extends NotDeleted

case object EntityNotFound extends NotDeleted
