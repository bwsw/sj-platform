Deployment of Platform
==========================

.. warning:: The section is under development!

Overall Deployment Infrastructure
--------------------------------------------

.. warning:: The section is under development!

The Stream Juggler platform needs the following infrastructure components to be deployed:

- `Apache Mesos <http://mesos.apache.org/>`_  for resource management that allows to run the system at scale and to support different types of workloads.

- `Docker <http://mesos.apache.org/documentation/latest/docker-containerizer/>`_ to start applicable services in Mesos cloud. 

- `Marathon <https://mesosphere.github.io/marathon/>`_ that provides support for Mesos containers and Docker and allows to run long-life tasks as well.

- `Chronos <https://mesos.github.io/chronos/>`_ is used for starting periodic tasks

- `ZooKeeper <https://zookeeper.apache.org/>`_ is used to perform leader election in the event that the currently leading Marathon instance fails. ZooKeeper is also responsible for instance task synchronization for a Batch module.

- `Mesos+Consul <https://github.com/CiscoCloud/mesos-consul>`_ is used for base service search.

- Data sources for the platform are `Netty <https://netty.io/>`_ and `T-streams <https://t-streams.com>`_ libraries and `Kafka <https://kafka.apache.org/>`_. For starting Kafka `Kafka on Mesos <https://github.com/mesos/kafka>`_ is used.

- Elasticsearch, JDBC or REST are external storages the output data is stored to.

- We use `MongoDB <https://www.mongodb.com/>`_ as a document database that provides high performance and availability. To start MongoDB in Mesos we use `MongoDB-Marathon Docker <https://hub.docker.com/r/tobilg/mongodb-marathon/>`_

- A custom-container on `NGINX <https://www.nginx.com>`_ is used for external access. 

The platform kernel is coded in Scala.

The UI is presented via Node JS.


Mesos Deployment
~~~~~~~~~~~~~~~~~~~~~~~~

.. warning:: The section is under development!

Mesos framework
""""""""""""""""""""""""
.. warning:: The section is under development!

Minimesos Deployment
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. warning:: The section is under development!

Requirements: 

- git,
- sbt (http://www.scala-sbt.org/download.html),
- docker,
- curl

1) Pull and assemble the first project::

    git clone https://github.com/bwsw/sj-platform.git
    cd sj-platform
    git checkout develop

    sbt sj-mesos-framework/assembly
    sbt sj-input-streaming-engine/assembly
    sbt sj-regular-streaming-engine/assembly
    sbt sj-output-streaming-engine/assembly

    cd ..

2) Pull and assemble the second project::

    git clone https://github.com/bwsw/sj-fping-demo.git
    cd sj-fping-demo
    git checkout develop

    sbt assembly

    cd ..

3) Install minimesos::
 
    curl -sSL https://minimesos.org/install | sh

 This command will display in terminal result like::

   Run the following command to add it to your executables path:
   export PATH=$PATH:/root/.minimesos/bin

 Create a directory to place all minimesos-related files::

   mkdir ~/minimesos
   cd ~/minimesos

 Then you need to create minimesosFile::
 
   touch minimesosFile

 and place into it all following settings::

   minimesos {
    clusterName = "Minimesos Cluster"
    loggingLevel = "INFO"
    mapAgentSandboxVolume = false
    mapPortsToHost = true
    mesosVersion = "1.0.0"
    timeout = 60

    agent {
        imageName = "containersol/mesos-agent"
        imageTag = "1.0.0-0.1.0"
        loggingLevel = "# INHERIT FROM CLUSTER"
        portNumber = 5051

        resources {

            cpu {
                role = "*"
                value = 4
            }

            disk {
                role = "*"
                value = 200
            }

            mem {
                role = "*"
                value = 8192
            }

            ports {
                role = "*"
                value = "[31000-32000]"
            }
        }
    }

    consul {
        imageName = "consul"
        imageTag = "0.7.1"
    }

    marathon {
        cmd = "--master zk://minimesos-zookeeper:2181/mesos --zk zk://minimesos-zookeeper:2181/marathon"
        imageName = "mesosphere/marathon"
        imageTag = "v1.3.5"

        // Add 'app { marathonJson = "<path or URL to JSON file>" }' for every task you want to execute
        app {
            marathonJson = "https://raw.githubusercontent.com/ContainerSolutions/minimesos/e2a43362f4581122762c80d8780d09b567783f1a/apps/weave-scope.json"
        }
    }

    master {
        aclJson = null
        authenticate = false
        imageName = "containersol/mesos-master"
        imageTag = "1.0.0-0.1.0"
        loggingLevel = "# INHERIT FROM CLUSTER"
    }

    mesosdns {
        imageName = "xebia/mesos-dns"
        imageTag = "0.0.5"
    }


    registrator {
        imageName = "gliderlabs/registrator"
        imageTag = "v6"
    }

    zookeeper {
        imageName = "jplock/zookeeper"
        imageTag = "3.4.6"
    }
  }

4) Deploy minimesos::

    minimesos up

 Try to launch minimesos until you will see the following result (it can differ from the example because IPs can differ)::

  export MINIMESOS_NETWORK_GATEWAY=172.17.0.1
  export MINIMESOS_AGENT=http://172.17.0.7:5051; export  MINIMESOS_AGENT_IP=172.17.0.7
  export MINIMESOS_ZOOKEEPER=zk://172.17.0.3:2181/mesos; export MINIMESOS_ZOOKEEPER_IP=172.17.0.3
  export MINIMESOS_MARATHON=http://172.17.0.6:8080; export MINIMESOS_MARATHON_IP=172.17.0.6
  export MINIMESOS_CONSUL=http://172.17.0.8:8500; export MINIMESOS_CONSUL_IP=172.17.0.8
  export MINIMESOS_MESOSDNS=http://172.17.0.4:53; export MINIMESOS_MESOSDNS_IP=172.17.0.4
  export MINIMESOS_MASTER=http://172.17.0.5:5050; export MINIMESOS_MASTER_IP=172.17.0.5

 Execute all mentioned lines (export all variables with corresponding values and execute the command from the last line).

 If the result is not the same (for the absence of the last line or/and lack of some exports) you shall execute the following command::

  minimesos destroy

 and try to launch minimesos again.

 After running minimesos, install dnsmasq::
  
  sudo apt-get install dnsmasq

 After launching you can see weavescope app (https://github.com/weaveworks/scope) on port 4040.

 This application is an instrument to visualize, monitor your docker containers. It generates the map that can look like at the picture below:

 .. _static/wavescope4.png

 Besides you can obtain access to Mesos on port 5050:

 .. _static/

 and also access to Marathon on port 8080:

 .. _static/

 Check dns by ping master node::

  ping -c 4 master.mm

 At the end you can see::

  4 packets transmitted, 4 received, 0% packet loss


5) Deploy services

 Create the following files in the minimesos folder (mongo.json, sj-rest.json, etc.) and run services with the provided commands.

 In each file you shall perform some replacements:

 - use value of the MINIMESOS_ZOOKEEPER_IP variable (can be found in the previous step) instead of <zk-ip>

 - use value of the MINIMESOS_MESOSDNS_IP variable (can be found in the previous step) instead of <dns-ip>

 Instead of creating each file with appropriate values by hand you may use the provided script (createAlLConfigs.sh) which shall be executed in the minimesos folder.

 After deploying each service you may see corresponding applications in Marathon UI (port 8080) and corresponding tasks in Mesos UI (port 5050). The graph structure provided by weavescope will surely change (port 4040).

 Marathon

 .. _static/


 Mesos

 .. _static
 
 Wavescope

 .. _static/

* mongo.json

``minimesos install --marathonFile mongo.json`` ::

 {  
   "id":"mongo",
   "container":{  
      "type":"DOCKER",
      "volumes": [
        {
          "containerPath": "/data/db",
          "hostPath": "mongo_data",
          "mode": "RW"
        }
      ],
      "docker":{  
         "image":"mongo",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":27017,
               "hostPort":0,
               "protocol":"tcp"
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always"
            },
 	    {
		"key":"dns",
		"value": <dns-ip>
	    }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":512
 }




* sj-rest.json

``minimesos install --marathonFile sj-rest.json`` ::

 {  
   "id":"sj-rest",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"bwsw/sj-rest:dev",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":8080,
               "hostPort":0,
               "protocol":"tcp"
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always"
            },
            {  
               "key":"dns",
               "value": <dns-ip>
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":1024,
   "env":{
      "MONGO_HOSTS":"mongo.marathon.mm:27017",
      "ZOOKEEPER_HOST":"<zk-ip>",
      "ZOOKEEPER_PORT":"2181"
   }
 }



* kafka.json

``minimesos install --marathonFile kafka.json`` ::

 {  
   "id":"kafka",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"ches/kafka",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":9092,
               "hostPort":0,
               "servicePort":9092,
               "protocol":"tcp"
            },
		{  
               "containerPort":7203,
               "hostPort":0,
               "servicePort":7203,
               "protocol":"tcp"
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always"
            },
            {
		"key":"dns",
		"value": <dns-ip>
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.2,
   "mem":512,
   "env":{  
      "ZOOKEEPER_IP":"<zk-ip>",
      "KAFKA_ADVERTISED_HOST_NAME":"kafka"
   }
 }



* elasticsearch.json

``sudo sysctl -w vm.max_map_count=262144``
``minimesos install --marathonFile elasticsearch.json`` ::

 {   
   "id":"elasticsearch",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"elasticsearch:5.1.1",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":9200,
               "hostPort":0,
               "protocol":"tcp" 
            },
        {  
               "containerPort":9300,
               "hostPort":0,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            },
            {  
               "key":"dns",
               "value": <dns-ip>
            }
         ]
      }
   },
   "args": ["-Etransport.host=0.0.0.0", "-Ediscovery.zen.minimum_master_nodes=1"],
   "instances":1,
   "cpus":0.2,
   "mem":2560
 }



* kibana.json

``minimesos install --marathonFile kibana.json`` ::

 {  
   "id":"kibana",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"kibana:5.1.1",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":5601,
               "hostPort":0,
               "protocol":"tcp"
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always"
            },
 	    {
		"key":"dns",
		"value": <dns-ip>
	    }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":256,
   "env":{  
      "ELASTICSEARCH_URL":"http://elasticsearch.marathon.mm:9200"
   }
 }



* config.properties

In this file instead of <path_to_conf_directory> you shall specify path to directory with config.properties file ::

 key=pingstation
 active.tokens.number=100
 token.ttl=120

 host=0.0.0.0
 port=8080
 thread.pool=4

 path=/tmp
 data.directory=transaction_data
 metadata.directory=transaction_metadata
 commit.log.directory=commit_log
 commit.log.rocks.directory=commit_log_rocks

 berkeley.read.thread.pool = 2

 counter.path.file.id.gen=/server_counter/file_id_gen

 auth.key=dummy
 endpoints=127.0.0.1:31071
 name=server
 group=group

 write.thread.pool=4
 read.thread.pool=2
 ttl.add-ms=50
 create.if.missing=true
 max.background.compactions=1
 allow.os.buffer=true
 compression=LZ4_COMPRESSION
 use.fsync=true

 zk.endpoints=172.17.0.3:2181
 zk.prefix=/pingstation
 zk.session.timeout-ms=10000
 zk.retry.delay-ms=500
 zk.connection.timeout-ms=10000
 
 max.metadata.package.size=100000000
 max.data.package.size=100000000
 transaction.cache.size=300
 
 commit.log.write.sync.value = 1
 commit.log.write.sync.policy = every-nth
 incomplete.commit.log.read.policy = skip-log
 commit.log.close.delay-ms = 200
 commit.log.file.ttl-sec = 86400
 stream.zookeeper.directory=/tts/tstreams

 ordered.execution.pool.size=2
 transaction-database.transaction-keeptime-min=70000
 subscribers.update.period-ms=500

* tts.json

``minimesos install --marathonFile tts.json`` ::

 {
    "id": "tts",
    "container": {
        "type": "DOCKER",
        "volumes": [
            {
                "containerPath": "/etc/conf",
                "hostPath": "<path_to_conf_directory>",
                "mode": "RO"
            }
        ],
        "docker": {
            "image": "bwsw/tstreams-transaction-server",
            "network": "BRIDGE",
            "portMappings": [
                {
                    "containerPort": 8080,
                    "hostPort": 31071,
                    "protocol": "tcp"
                }
            ],
            "parameters": [
                {
                    "key": "restart",
                    "value": "always"
                }
            ]
        }
    },
    "instances": 1,
    "cpus": 0.1,
    "mem": 512,
    "env": {
      "HOST":"172.17.0.1",
      "PORT0":"31071"
    }
 }



6) Upload engine jars::

    cd  sj-platform

    address=sj-rest.marathon.mm:8080

    curl --form jar=@core/sj-mesos-framework/target/scala-2.12/sj-mesos-framework-1.0- SNAPSHOT.jar http://$address/v1/custom/jars
    curl --form jar=@core/sj-input-streaming-engine/target/scala-2.12/sj-input-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
    curl --form jar=@core/sj-regular-streaming-engine/target/scala-2.12/sj-regular-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
    curl --form jar=@core/sj-output-streaming-engine/target/scala-2.12/sj-output-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars

7) Setup settings for engine::

    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"session-timeout\",\"value\": \"7000\",\"domain\": \"zk\"}"
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"current-framework\",\"value\": \"com.bwsw.fw-1.0\",\"domain\": \"system\"}"

    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-host\",\"value\": \"sj-rest.marathon.mm\",\"domain\": \"system\"}"
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-port\",\"value\": \"8080\",\"domain\": \"system\"}"

    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect\",\"value\": \"http://marathon.mm:8080\",\"domain\": \"system\"}"
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect-timeout\",\"value\": \"60000\",\"domain\": \"system\"}"
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"kafka-subscriber-timeout\",\"value\": \"100\",\"domain\": \"system\"}"
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"low-watermark\",\"value\": \"100\",\"domain\": \"system\"}" 

    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"regular-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.RegularInstanceValidator\",\"domain\": \"system\"}"
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"input-streaming-validator-   class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.InputInstanceValidator\",\"domain\": \"system\"}"
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"output-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.OutputInstanceValidator\",\"domain\": \"system\"}"

8) Now modules can be set up::

    cd ..
    cd sj-fping-demo

.. _Create_Platform_Entites:

SJ-Platform Entities Deployment 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once all platform prerequisites are set up, platform entities can be added to the system.

Below the steps for creating the platform entities such as providers, services, streams, instances via REST API methods can be found.

1) Set up providers::

    sed -i 's/176.120.25.19/elasticsearch.marathon.mm/g' api-json/providers/elasticsearch-ps-provider.json
    curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers  /elasticsearch-ps-provider.json"

    sed -i 's/176.120.25.19/kafka.marathon.mm/g' api-json/providers/kafka-ps-provider.json
    curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data  "@api-json/providers/kafka-ps-provider.json"

    sed -i 's/176.120.25.19/$MINIMESOS_ZOOKEEPER_IP/g' api-json/providers/zookeeper-ps-provider.json
    curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/zookeeper-ps-provider.json"

2) Next set up services::

     curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/elasticsearch-ps-service.json"
     curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/kafka-ps-service.json"
     curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/zookeeper-ps-service.json"
     curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/tstream-ps-service.json"

3) Compile and upload modules jars::

     curl "https://oss.sonatype.org/content/repositories/snapshots/com/bwsw/sj-regex-input_2.12/1.0-SNAPSHOT/sj-regex-input_2.12-1.0-SNAPSHOT.jar" -o sj-regex-input.jar
     curl --form jar=@sj-regex-input.jar http://$address/v1/modules
     curl --form jar=@ps-process/target/scala-2.12/ps-process-1.0-SNAPSHOT.jar http://$address/v1/modules
     curl --form jar=@ps-output/target/scala-2.12/ps-output-1.0-SNAPSHOT.jar http://$address/v1/modules

4) Create streams::

     curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response.json"
     curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/unreachable-response.json"
     curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response-1m.json"
     curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/es-echo-response-1m.json"
     curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/fallback-response.json"

5) Create instances::

     curl --request POST "http://$address/v1/modules/input-streaming/com.bwsw.input.regex/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-input.json"
     curl --request POST "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-echo-process.json"
     curl --request POST "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-output.json"

6) Create the index and the mapping::

     curl --request PUT "http://176.120.25.19:9200/pingstation" -H 'Content-Type: application/json' --data "@api-json/elasticsearch-index.json"


7) Ready! Now you can launch modules::

     curl --request GET "http://$address/v1/modules/input-streaming/com.bwsw.input.regex/1.0/instance/pingstation-input/start"
     curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-echo-process/start"
     curl --request GET "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output/start"

How to obtain results of work:

- docker ps

Find port of kibana:

 384b7644b034 kibana:5.1.1 "/docker-entrypoint.s" About an hour ago Up About an hour   0.0.0.0:31570->5601/tcp mesos-b367c0cd-70f2-4e3c-a57d-147c516f2855-S0.db48d51e-3b10-42da-b22d-d92cc2a8f94e

Here it is 31570. And then go to <ip of your machine>:31570. There you shall enter "pingstation" and select "ts" to create default index pattern.

Then click Discover tab.    




SJ-Platform Entities Deployment 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. warning:: The section is under development!
