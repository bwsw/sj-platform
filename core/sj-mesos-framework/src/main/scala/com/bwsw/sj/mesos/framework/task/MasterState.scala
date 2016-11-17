package com.bwsw.sj.mesos.framework.task

/**
  * Created by diryavkin_dn on 01.11.16.
  */
class MasterState {
  var start_time:Double = _
  var hostname:String = _
  var git_tag:String = _
  var flags:Map[String,Any] = _
  var elected_time:Double = _
  var unregistered_frameworks:Array[String] = _
  var frameworks:Array[Framework] = _
  var deactivated_slaves:Double = _
  var git_sha:String = _
  var build_date:String = _
  var orphan_tasks:Array[Map[String,Any]] = _
  var leader:String = _
  var completed_frameworks:Array[Framework] = _
  var version:String = _
  var id:String = _
  var pid:String = _
  var build_user:String = _
  var build_time:Double = _
  var activated_slaves:Double = _
  var slaves:Array[Slave] = _
}


class Slave {
  var hostname:String = _
  var registered_time:Double = _
  var offered_resources:Map[String,Any] = _
  var attributes:Map[String,Any] = _
  var version:String = _
  var id:String = _
  var pid:String = _
  var reserved_resources:Map[String,Any] = _
  var unreserved_resources:Map[String,Any] = _
  var resources:Map[String,Any] = _
  var used_resources:Map[String,Any] = _
  var reregistered_time:Double = _
  var active:Boolean = _
}
