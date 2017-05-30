package com.bwsw.sj.common

import scaldi.Injector

trait SjInjector {
  implicit val injector: Injector
}
