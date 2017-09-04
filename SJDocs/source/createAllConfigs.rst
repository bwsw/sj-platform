Script for Creating Service Configurations
========================================================

Copy and paste into the "createAllConfigs.sh" text file the following content::

 #!/bin/sh

 cat <<EOT >> mongo.json
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
        "value": "$MINIMESOS_MESOSDNS_IP"
        }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":512
 }
 EOT

 cat <<EOT >> sj-rest.json
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
               "value": "$MINIMESOS_MESOSDNS_IP"
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":1024,
   "env":{
      "MONGO_HOSTS":"mongo.marathon.mm:27017",
      "ZOOKEEPER_HOST":"$MINIMESOS_ZOOKEEPER_IP",
      "ZOOKEEPER_PORT":"2181" 
   }
 }
 EOT

 cat <<EOT >> kafka.json
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
        "value": "$MINIMESOS_MESOSDNS_IP"
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.2,
   "mem":512,
   "env":{  
      "ZOOKEEPER_IP":"$MINIMESOS_ZOOKEEPER_IP",
      "KAFKA_ADVERTISED_HOST_NAME":"kafka" 
   }
 }
 EOT

 cat <<EOT >> elasticsearch.json
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
 EOT

 cat <<EOT >> kibana.json
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
        "value": "$MINIMESOS_MESOSDNS_IP"
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
 EOT

 cat <<EOT >> config.properties
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

 zk.endpoints=$MINIMESOS_ZOOKEEPER_IP:2181
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
 EOT

 export PATH_TO_CONF_DIR="`pwd`"
 cat <<EOT >> tts.json
 {
    "id": "tts",
    "container": {
        "type": "DOCKER",
        "volumes": [
            {
                "containerPath": "/etc/conf",
                "hostPath": "$PATH_TO_CONF_DIR",
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
 EOT
