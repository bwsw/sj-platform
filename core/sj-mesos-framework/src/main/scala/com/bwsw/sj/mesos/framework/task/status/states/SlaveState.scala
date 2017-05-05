package com.bwsw.sj.mesos.framework.task.status.states

class SlaveState(
                  var start_time: Any,
                  var hostname: Any,
                  var master_hostname: Any,
                  var git_tag: Any,
                  var flags: Any,
                  var log_dir: Any,
                  var frameworks: Array[Framework],
                  var git_sha: Any,
                  var build_date: Any,
                  var attributes: Any,
                  var completed_frameworks: Any,
                  var version: Any,
                  var id: String,
                  var pid: String,
                  var build_user: Any,
                  var resources: Any,
                  var build_time: Any)

class Framework(
                 var capabilities: Array[Any],
                 var name: String,
                 var completed_executors: Array[Map[String, Any]],
                 var hostname: String,
                 var role: String,
                 var registered_time: Double,
                 var unregistered_time: Double,
                 var executors: Array[Executor],
                 var completed_tasks: Array[Any],
                 var offered_resources: Map[String, Any],
                 var id: String,
                 var offers: Array[Any],
                 var tasks: Array[Map[String, Any]],
                 var pid: String,
                 var failover_timeout: Double,
                 var principal: String,
                 var checkpoint: Boolean,
                 var resources: Map[String, Any],
                 var used_resources: Map[String, Any],
                 var webui_url: String,
                 var user: String,
                 var active: Boolean,
                 var reregistered_time: Double
               )

class Executor(
                var name: String,
                var source: String,
                var container: String,
                var completed_tasks: Array[Map[String, Any]],
                var id: String,
                var tasks: Array[Map[String, Any]],
                var resources: Map[String, Any],
                var queued_tasks: Array[Map[String, Any]],
                var directory: String)
