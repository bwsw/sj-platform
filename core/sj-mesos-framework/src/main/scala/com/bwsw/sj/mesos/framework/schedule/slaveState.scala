package com.bwsw.sj.mesos.framework.schedule


/**
  * Created by diryavkin_dn on 01.11.16.
  */
class slaveState() {
  var start_time:Any = _
  var hostname:Any = _
  var master_hostname:Any = _
  var git_tag:Any = _
  var flags:Any = _
  var log_dir:Any = _
  var frameworks:Array[framework] = _
  var git_sha:Any = _
  var build_date:Any = _
  var attributes:Any = _
  var completed_frameworks:Any = _
  var version:Any = _
  var id:String = _
  var pid:String = _
  var build_user:Any = _
  var resources:Any = _
  var build_time:Any = _
}

class framework() {
  var capabilities:Array[Any] = _
  var name:String = _
  var completed_executors:Array[Map[String, Any]] = _
  var hostname:String = _
  var role:String = _
  var registered_time:Double = _
  var unregistered_time:Double = _
  var executors:Array[executor] = _
  var completed_tasks:Array[Any] = _
  var offered_resources:Map[String,Any] = _
  var id:String = _
  var offers:Array[Any] = _
  var tasks:Array[Map[String,Any]] = _
  var pid:String = _
  var failover_timeout:Double = _
  var principal:String = _
  var checkpoint:Boolean = _
  var resources:Map[String,Any] = _
  var used_resources:Map[String,Any] = _
  var webui_url:String = _
  var user:String = _
  var active:Boolean = _
  var reregistered_time:Double = _
}

class executor() {
  var name:String = _
  var source:String = _
  var container:String = _
  var completed_tasks:Array[Map[String, Any]] = _
  var id:String = _
  var tasks:Array[Map[String, Any]] = _
  var resources:Map[String, Any] = _
  var queued_tasks:Array[Map[String, Any]] = _
  var directory:String = _
}