Platform Deployment on Cluster 
=====================================

.. Contents::

The section provides a detailed step-by-step instruction on Stream Juggler Platform deployment on cluster. 

Currently, the deployment is supported on Mesos.

A complete list of requirements and the deployment procedure description can be found below. This is a demo case for which all the services are deployed and started. Then entities are added to the platform - providers, services, streams, modules and instances. These entities are also of the particular types necessary to solve the demo task.

Overall Deployment Infrastructure
--------------------------------------------

.. warning:: The section is under development!

The Stream Juggler platform needs the following services to be preliminarily deployed:

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

Below, you will find necessary instructions to run the services. Two deployment options are described next - on cluster (Mesos) and locally (on minimesos). The steps to deploy services, SJ-Platform, entities for the platform are provided.

Mesos Deployment
~~~~~~~~~~~~~~~~~~~~~~~~

The deployment is performed via REST API.

Firstly, deploy Mesos and other services.

1. Deploy Mesos, Marathon, Zookeeper. You can follow the instructions in the official `instalation guide <http://www.bogotobogo.com/DevOps/DevOps_Mesos_Install.php>`_ .

Please, note, the deployment is described for one default Mesos-slave with available ports [31000-32000]. 

2. Docker containers should be supported for Mesos-slave. For Docker deployment follow the instructions at the official `installation guide <https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/#install-docker-ce>`_

3. Install Java::
                                         
    $ sudo add-apt-repository ppa:webupd8team/java
    $ sudo apt-get update
    $ sudo apt-get install oracle-java8-installer
    $ sudo apt-get install oracle-java8-set-default

   Find detailed instructions on Java deployment in the `installation guide <https://tecadmin.net/install-oracle-java-8-ubuntu-via-ppa/>`_.

4. Start Mesos-master, Mesos-slave and the services. 

After performing all the steps, make sure you have access to Mesos interface, Marathon interface. Zookeeper now should be active.


5. Create json files and a configuration file (config.properties). Please, name them as it is specified here.

**mongo.json**::

 {  
   "id":"mongo",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"mongo:3.4.7",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":27017,
               "hostPort":31027,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":512
 }

**sj-rest.json**

Replace <slave_advertise_ip> with a valid slave advertise IP.
Replace <zk_ip> and <zk_port> according to the zookeeper address::

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
               "hostPort":31080,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":1024,
   "env":{
      "MONGO_HOSTS":"<slave_advertise_ip>:31027",
      "ZOOKEEPER_HOST":"<zk_ip>",
      "ZOOKEEPER_PORT":"<zk_port>" 
   }
 }

For sj-rest.json it is better to upload the docker image separately::
 
 $ sudo docker pull bwsw/sj-rest:dev

**kafka.json**::

Please, replace <zk_ip> according to the zookeeper address::

 {  
   "id":"kafka",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"ches/kafka:0.10.2.1",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":9092,
               "hostPort":31992,
               "servicePort":9092,
               "protocol":"tcp" 
            },
        {  
               "containerPort":7203,
               "hostPort":31723,
               "servicePort":7203,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":512,
   "env":{  
      "ZOOKEEPER_IP":"<zk_ip>",
      "KAFKA_ADVERTIZEED_HOST_NAME":"kafka" 
   }
 }

**elasticsearch.json**::

 {  
   "id":"elasticsearch",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"docker.elastic.co/elasticsearch/elasticsearch:5.5.1",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":9200,
               "hostPort":31920,
               "protocol":"tcp" 
            },
        {  
               "containerPort":9300,
               "hostPort":31930,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "env":{  
      "http.host":"0.0.0.0", 
      "xpack.security.enabled":"false", 
      "transport.host":"0.0.0.0", 
      "cluster.name":"elasticsearch" 
   },
   "instances":1,
   "cpus":0.2,
   "mem":256
 }

**Configuration properties** 

Replace <zk_ip> according to the zookeeper address::

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

 zk.endpoints=<zk_ip>
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



**tts.json** 

This is a json file for T-streams. Please, replace <path_to_conf_directory> with an appropriate path to the configuration file directory on your computer. Also replace <slave_advertise_ip> with slave advertise IP::

 {
    "id": "tts",
    "container": {
        "type": "DOCKER",
        "volumes": [
            {
                "containerPath": "/etc/conf/config.properties",
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
      "HOST":"<slave_advertise_ip>",
      "PORT0":"31071" 
    }
}

**kibana.json** (<slave_advertise_ip> should be replaced with slave advertise IP)::

 {  
   "id":"kibana",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"kibana:5.5.1",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":5601,
               "hostPort":31561,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":256,
   "env":{  
      "ELASTICSEARCH_URL":"https://<slave_advertise_ip>:31920" 
   }
 }

3. Run the services on Marathon.

Replace <marathon_address> with a valid Marathon address.

**Mongo**::
 
 $ curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @mongo.json 

**SJ-rest**::

 $ curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @sj-rest.json  

**Elasticsearch**:

Please, note that `vm.max_map_count` should be specified for slave::

 $ sudo sysctl -w vm.max_map_count=262144

Then launch Elasticsearch::

 $ curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @elasticsearch.json

**Kafka**::

 $ curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @kafka.json 

**tts**::
 
 $ curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @tts.json 

**Kibana**::

 $ curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @kibana.json

Via the Marathon interface make sure the services are deployed.

6. Add the settings if running the framework on Mesos needs principal/secret:: 

   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"framework-principal\",\"value\": <principal>,\"domain\": \"configuration.system\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"framework-secret\",\"value\": <secret>,\"domain\": \"configuration.system\"}" 

7. Copy the project for the GitHub repository of the SJ-Platform::

   $ git clone https://github.com/bwsw/sj-platform.git


Now look and make sure you have access to the Web UI. You will see the platform but it is not completed with any entities yet. They will be added in the next steps.

Configurations Uploading
""""""""""""""""""""""""""
Before uploading modules, compile and upload the engine jars for them. 

1. Please, upload the engine jars for the modules (input-streaming, regular-streaming, output-streaming) and a Mesos framework. Please,  replace <slave_advertise_ip> with slave advertise IP::

    $ cd sj-platform
    $ address=<slave_advertise_ip>:31080
    $ curl --form jar=@core/sj-mesos-framework/target/scala-2.12/sj-mesos-framework-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
   $ curl --form jar=@core/sj-input-streaming-engine/target/scala-2.12/sj-input-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
   $ curl --form jar=@core/sj-regular-streaming-engine/target/scala-2.12/sj-regular-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
   $ curl --form jar=@core/sj-output-streaming-engine/target/scala-2.12/sj-output-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars

2. Setup configurations for engines.

The range of configurations includes required and optional ones. 

The list of all configurations can be viewed at the :ref:`Configuration` page.

To set up configuration settings for the engines, run the following commands. Please, replace <rest_ip> with the IP of REST and <marathon_address> with the address of Marathon::

   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"session-timeout\",\"value\": \"7000\",\"domain\": \"configuration.apache-zookeeper\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"current-framework\",\"value\": \"com.bwsw.fw-1.0\",\"domain\": \"configuration.system\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-host\",\"value\": \"<rest_ip>\",\"domain\": \"configuration.system\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-port\",\"value\": \"31080\",\"domain\": \"configuration.system\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect\",\"value\": \"http://<marathon_address>\",\"domain\": \"configuration.system\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect-timeout\",\"value\": \"60000\",\"domain\": \"configuration.system\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"kafka-subscriber-timeout\",\"value\": \"100\",\"domain\": \"configuration.system\"}" 
   $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"low-watermark\",\"value\": \"100\",\"domain\": \"configuration.system\"}" 

3. Send the next POST requests to upload configurations for module validators::

    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"regular-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.RegularInstanceValidator\",\"domain\": \"configuration.system\"}" 
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"input-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.InputInstanceValidator\",\"domain\": \"configuration.system\"}" 
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"output-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.OutputInstanceValidator\",\"domain\": \"configuration.system\"}" 

In the UI you can see the uploaded configurations under the “Configuration” tab of the main navigation.

Module Uploading
""""""""""""""""""""""""""

1. Copy demo project repository::

   $ cd ..
   $ git clone https://github.com/bwsw/sj-fping-demo.git
   $ cd sj-fping-demo

2. Compile and upload modules jars:: 

    $ sbt assembly
    $ curl "https://oss.sonatype.org/content/repositories/snapshots/com/bwsw/sj-regex-input_2.12/1.0-SNAPSHOT/sj-regex- input_2.12-1.0-SNAPSHOT.jar" -o sj-regex-input.jar
    $ curl --form jar=@sj-regex-input.jar http://$address/v1/modules
    $ curl --form jar=@ps-process/target/scala-2.12/ps-process-1.0-SNAPSHOT.jar http://$address/v1/modules
    $ curl --form jar=@ps-output/target/scala-2.12/ps-output-1.0-SNAPSHOT.jar http://$address/v1/modules

Stream Creation
""""""""""""""""""""""""""""""

1. Set up providers.

There is a default value of Elasticsearch, Apache Kafka and Apache Zookeeper IPs (176.120.25.19) in json configuration files. So you shall change it appropriately via `sed` app before using (replace the <slave_advertise_ip> placeholder)::

 $ sed -i 's/176.120.25.19:9300/<slave_advertise_ip>:31930/g' api-json/providers/elasticsearch-ps-provider.json
 $ curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/elasticsearch-ps-provider.json" 
 $ sed -i 's/176.120.25.19:9092/<slave_advertise_ip>:31992/g' api-json/providers/kafka-ps-provider.json
 $ curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/kafka-ps-provider.json" 
 $ sed -i 's/176.120.25.19:2181/<slave_advertise_ip>/g' api-json/providers/zookeeper-ps-provider.json
 $ curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/zookeeper-ps-provider.json" 


2. Next set up services::

    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/elasticsearch-ps-service.json" 
    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/kafka-ps-service.json" 
    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/zookeeper-ps-service.json" 
    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/tstream-ps-service.json" 


3. Create streams (replace <stream_name> with a name of the stream json file)::

    $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response.json" 
    $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/unreachable-response.json" 
    $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response-1m.json" 
    $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/es-echo-response-1m.json" 
    $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/fallback-response.json" 


4. Create output destination.

At this step all necessary indexes, tables and mapping should be created for storing the processed result.

In our demo case the destination is of Elasticsearch type. Thus, the index and the mapping should be created. Please, run the command below. Do not forget to replace <slave_advertise_ip> with the advertise IP of Mesos-slave::

 $curl --request PUT "http://<slave_advertise_ip>:31920/pingstation" -H 'Content-Type: application/json' --data "@api-json/elasticsearch-index.json" 


Instance Creation
""""""""""""""""""""""""""""

Create instances for modules. Please, replace <module_name> with the name of the module the instance is created for, <instance_name> with a name of the instance::

 $ curl --request POST "http://$address/v1/modules/input-streaming/com.bwsw.input.regex/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-input.json" 
 $ curl --request POST "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-echo-process.json" 
 $ curl --request POST "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-output.json" 
 
Instance Launching
""""""""""""""""""""""""

Launch the created instances by sending GET request for each instance (please, replace <instance_name> and <module_name> with the name of the instance and the name of its module)::

 $ curl --request GET "http://$address/v1/modules/input-streaming/com.bwsw.input.regex/1.0/instance/pingstation-input/start" 
 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-echo-process/start" 
 $ curl --request GET "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output/start" 

 
Now the data can be delevered into the system. The instance(-s) starts data processing. 

View, monitor and manage instance performance via the UI.


Minimesos Deployment
~~~~~~~~~~~~~~~~~~~~~~~~~~

The following services are required before the platfrom deployment on minimesos: 

- git,
- sbt (find downloading instructions `here <http://www.scala-sbt.org/download.html>`_),
- Docker,
- cURL

1) Pull and assemble the SJ-Platform project::

    git clone https://github.com/bwsw/sj-platform.git
    cd sj-platform
    git checkout develop

    sbt sj-mesos-framework/assembly
    sbt sj-input-streaming-engine/assembly
    sbt sj-regular-streaming-engine/assembly
    sbt sj-output-streaming-engine/assembly

    cd ..

2) Pull and assemble the demo project::

    git clone https://github.com/bwsw/sj-fping-demo.git
    cd sj-fping-demo
    git checkout develop

    sbt assembly

    cd ..

3) Install minimesos::
 
    curl -sSL https://minimesos.org/install | sh

   This command will be displayed in the terminal result::

    Run the following command to add it to your executables path:
    export PATH=$PATH:/root/.minimesos/bin

   You should execute this export command::
  
    export PATH=$PATH:/root/.minimesos/bin

   Also, you can append this command to the end of file ~/.profile to have this instruction executed on each login. 

   Create a directory to place all minimesos-related files::

    mkdir ~/minimesos
    cd ~/minimesos

   Then you need to create `minimesosFile`::
 
    touch minimesosFile

   Open the file to edit it::
  
    nano minimesosFile
 
   Copy and paste all the following settings into it::

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

    $ minimesos up

   Try to launch minimesos until you see the following result (IPs can differ from the provided example)::

    export MINIMESOS_NETWORK_GATEWAY=172.17.0.1
    export MINIMESOS_AGENT=http://172.17.0.7:5051; export MINIMESOS_AGENT_IP=172.17.0.7
    export MINIMESOS_ZOOKEEPER=zk://172.17.0.3:2181/mesos; export MINIMESOS_ZOOKEEPER_IP=172.17.0.3
    export MINIMESOS_MARATHON=http://172.17.0.6:8080; export MINIMESOS_MARATHON_IP=172.17.0.6
    export MINIMESOS_CONSUL=http://172.17.0.8:8500; export MINIMESOS_CONSUL_IP=172.17.0.8
    export MINIMESOS_MESOSDNS=http://172.17.0.4:53; export MINIMESOS_MESOSDNS_IP=172.17.0.4
    export MINIMESOS_MASTER=http://172.17.0.5:5050; export MINIMESOS_MASTER_IP=172.17.0.5
    Running dnsmasq? Add 'server=/mm/172.17.0.4#53' to /etc/dnsmasq.d/10-minimesos to resolve master.mm, zookeeper.mm and Marathon apps on app.marathon.mm.

   If the result is not the same (absence of the last line or/and lack of some exports) you shall execute the following command::

    $ minimesos destroy

   and try to launch minimesos again.

   Execute all the lines from the respond. First, export all variables with corresponding values.
 
   Then, execute the command from the last line. Open the file for editing::
 
    $ nano /etc/dnsmasq.d/10-minimesos
   
   Paste the line below into it (make sure the IP is the dns IP)::
  
    server=/mm/172.17.0.4#53
 
   After running minimesos, install dnsmasq::
  
    $ sudo apt-get install dnsmasq

   And launch it:: 
  
    $ sudo service dnsmasq restart
 
   After launching you can see weavescope app (https://github.com/weaveworks/scope) on port 4040.

   This application is an instrument to visualize, monitor your docker containers. It generates the map that can look like at the picture below: (image here)

   Besides you can obtain access to Mesos on port 5050: (image here)

   and also access to Marathon on port 8080: (image here)

   Check dns by ping master node::

    $ ping -c 4 master.mm

   At the end you can see::

    4 packets transmitted, 4 received, 0% packet loss


5) Deploy services

   Create the following files in the minimesos folder (mongo.json, sj-rest.json, etc.) and run services with the provided commands.

   In each file you shall perform some replacements:

    - use value of the MINIMESOS_ZOOKEEPER_IP variable (can be found in the previous step) instead of <zk-ip>

    - use value of the MINIMESOS_MESOSDNS_IP variable (can be found in the previous step) instead of <dns-ip>

   Instead of creating each file with appropriate values by hand you may use a script which shall be executed in the minimesos folder.
 
   Create a file named `createAlLConfigs.sh` with the following `content <>`_. Then execute it::
 
    $ ./createAlLConfigs.sh
 
   The json files will be created in the minimesos folder. All you need now is to deploy them to the system. Use the commands provided below for each json file.

  **mongo.json**::

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
         "image":"mongo:3.4.7",
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
        "value": "<dns-ip>" 
        }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":512
 }

And install it::
 
 $ minimesos install --marathonFile mongo.json


**sj-rest.json** (replace <dns-ip> and <zk-ip> with valid IPs)::

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

And install it::

 $ minimesos install --marathonFile sj-rest.json

**kafka.json** (replace <dns-ip> and <zk-ip> with valid IPs)::

 {  
   "id":"kafka",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"ches/kafka:0.10.2.1",
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
        "value": "<dns-ip>" 
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

And install it::

 $ minimesos install --marathonFile kafka.json

**elasticsearch.json** (replace <dns-ip> with a valid IP)::

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

And install it::

 $ sudo sysctl -w vm.max_map_count=262144
 $ minimesos install --marathonFile elasticsearch.json
 
**kibana.json** (replace <dns-ip> with a valid IP)::

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

And install it::

 $ minimesos install --marathonFile kibana.json


**config.properties**

In this file instead of <path_to_conf_directory> you shall specify path to directory with the `config.properties` file ::

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

**tts.json**::

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

And install it::

 $ minimesos install --marathonFile tts.json

 After deploying each service you may see corresponding applications in Marathon UI (port 8080) and corresponding tasks in Mesos UI (port 5050). The graph structure provided by weavescope will surely change (port 4040).

Configurations Uploading
""""""""""""""""""""""""

1. Upload engine jars in the next step::

    $ cd  sj-platform

    $ address=sj-rest.marathon.mm:8080

    $ curl --form jar=@core/sj-mesos-framework/target/scala-2.12/sj-mesos-framework-1.0- SNAPSHOT.jar http://$address/v1/custom/jars
    $ curl --form jar=@core/sj-input-streaming-engine/target/scala-2.12/sj-input-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
    $ curl --form jar=@core/sj-regular-streaming-engine/target/scala-2.12/sj-regular-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
    $ curl --form jar=@core/sj-output-streaming-engine/target/scala-2.12/sj-output-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars

2. Set up configurations for the engines::

    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"session-timeout\",\"value\": \"7000\",\"domain\": \"zk\"}"
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"current-framework\",\"value\": \"com.bwsw.fw-1.0\",\"domain\": \"system\"}"

    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-host\",\"value\": \"sj-rest.marathon.mm\",\"domain\": \"system\"}"
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-port\",\"value\": \"8080\",\"domain\": \"system\"}"

    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect\",\"value\": \"http://marathon.mm:8080\",\"domain\": \"system\"}"
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect-timeout\",\"value\": \"60000\",\"domain\": \"system\"}"
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"kafka-subscriber-timeout\",\"value\": \"100\",\"domain\": \"system\"}"
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"low-watermark\",\"value\": \"100\",\"domain\": \"system\"}" 

    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"regular-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.RegularInstanceValidator\",\"domain\": \"system\"}"
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"input-streaming-validator-   class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.InputInstanceValidator\",\"domain\": \"system\"}"
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"output-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.OutputInstanceValidator\",\"domain\": \"system\"}"

Modules Uploading
""""""""""""""""""""""""""

Now modules can be set up. 

1. Firtly, it necessary to move to the demo project directory::

    $ cd ..
    $ cd sj-fping-demo
 
2. Compile and upload modules' jars::

   $ curl "https://oss.sonatype.org/content/repositories/snapshots/com/bwsw/sj-regex-input_2.12/1.0-SNAPSHOT/sj-regex-input_2.12-1.0-SNAPSHOT.jar" -o sj-regex-input.jar
   $ curl --form jar=@sj-regex-input.jar http://$address/v1/modules
   $ curl --form jar=@ps-process/target/scala-2.12/ps-process-1.0-SNAPSHOT.jar http://$address/v1/modules
   $ curl --form jar=@ps-output/target/scala-2.12/ps-output-1.0-SNAPSHOT.jar http://$address/v1/modules


Stream Creation
"""""""""""""""""""""""""

The infrastructure for streams should be created first.

1. Set up providers.

There is a default value of Elasticsearch, Apache Kafka and Zookeeper IPs (176.120.25.19) in json configuration files, so we need to change it appropriately via sed app before using::

    $ sed -i 's/176.120.25.19/elasticsearch.marathon.mm/g' api-json/providers/elasticsearch-ps-provider.json
curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/elasticsearch-ps-provider.json" 

    $ sed -i 's/176.120.25.19/kafka.marathon.mm/g' api-json/providers/kafka-ps-provider.json
curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/kafka-ps-provider.json" 

    $ sed -i "s/176.120.25.19/${MINIMESOS_ZOOKEEPER_IP}/g" api-json/providers/zookeeper-ps-provider.json
curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/zookeeper-ps-provider.json" 

2. Then set up services::

    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/elasticsearch-ps-service.json" 
    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/kafka-ps-service.json" 
    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/zookeeper-ps-service.json" 
    $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/tstream-ps-service.json" 

3. Now sreate streams::

   $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response.json" 
   $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/unreachable-response.json" 
   $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response-1m.json" 
   $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/es-echo-response-1m.json" 
   $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/fallback-response.json" 
   
4. Create output destination.

At this step all necessary indexes, tables and mapping should be created for storing the processed result.

In our demo case the destination is of Elasticsearch type. Thus, the index and the mapping should be created. Please, run the command below. Do not forget to replace <slave_advertise_ip> with the advertise IP of Mesos-slave::

 $ curl --request PUT "http://elasticsearch.marathon.mm:9200/pingstation" -H 'Content-Type: application/json' --data "@api-json/elasticsearch-index.json" 

Instance Creation
""""""""""""""""""""""""

Create an instance for each module::

    $ curl --request POST "http://$address/v1/modules/input-streaming/com.bwsw.input.regex/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-input.json" 
    $ curl --request POST "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-echo-process.json" 
    $ curl --request POST "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-output.json" 

Instance Launching
"""""""""""""""""""""""""
Launch each instance::

 $ curl --request GET "http://$address/v1/modules/input-streaming/com.bwsw.input.regex/1.0/instance/pingstation-input/start" 
 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-echo-process/start" 
 $ curl --request GET "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output/start" 

View Results
""""""""""""""""""""""

To see the processing results saved in Elasticsearch, please, go to Kibana. There the aggregated data can be rendered in a diagram.

The result can be viewed while the module is working. A necessary auto-refresh interval can be set for the diagram to update the graph.

Firstly, click the **Settings** tab and fill in the data entry field '*' instead of 'logstash-*'. 

Then there will appear another data entry field called 'Time-field name'. You should choose 'ts' from the combobox and press the create button. 

After that, click the Discover tab. 

Choose a time interval of 'Last 15 minutes' in the top right corner of the page, as well as an auto-refresh interval of 45 seconds, as an example. Now a diagram can be compiled. 

Select the parameters to show in the graph at the left-hand panel. 

The example below is compiled in Kibana v.5.5.1.

It illustrates average time of echo-responses by IPs per a selected period of time (e.g. 1 min). As you can see, different nodes have the different average time of response. Some nodes respond faster than others. 

.. figure:: _static/Kibana.png

Lots of other parameter combinations can be implemented to view the results.

Instance Shutdown 
"""""""""""""""""""""""""

Once the task is resolved and necessary data is aggregated, the instances can be stopped. 

A stopped instance can be restarted again if it is necessary.

If there is no need for it anymore, a suspended instance can be deleted. On the basis of the uploaded modules and the whole created infrastructure (providers, services, streams) other instances can be created next time.

To stop instances in the example task the following requests should be sent::

 $ curl --request GET "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance/pingstation-input/stop"

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process/stop "

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-output/stop" 

In the UI, you will see the suspended instances with the “stopped” status.

.. figure:: _static/InstancesStopped.png

Instance Deleting 
""""""""""""""""""""""""""""

A stopped instance can be deleted if there is no need for it anymore. An instance of a specific module can be deleted via REST API by sending a DELETE request (as described below). Or instance deleting action is available in the UI under the “Instances” tab.

Make sure the instances to be deleted are stopped and are not with one of the following statuses: «starting», «started», «stopping», «deleting».

The instances of the modules can be deleted one by one::

 $ curl --request DELETE "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance/pingstation-input/"

 $ curl --request DELETE "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process/"

$ curl --request DELETE "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output/"

Via the UI you can make sure the instances are deleted.
