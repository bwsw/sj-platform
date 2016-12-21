# sj-platform
SJ-Platform repository holds source code for Stream Juggler Platform Event Processing Engine.

Launch rest in docker:
docker run -d --name rest-ui -e MONGO_HOSTS=<mongo hostS> -e ZOOKEEPER_HOST=<zk host> -e ZOOKEEPER_PORT=<zk port> -p 8080:8080 bwsw/sj-rest
