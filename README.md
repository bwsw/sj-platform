# sj-platform
SJ-Platform repository holds source code for Stream Juggler Platform Event Processing Engine.

Launch rest in docker:
docker run -d --name rest-ui -e MONGO_HOST=<mongo host> -e MONGO_PORT=<mongo port> -e ZOOKEEPER_HOST=<zk host> -e ZOOKEEPER_PORT=<mongo port> -p 8080:8080 bwsw/sj-rest
