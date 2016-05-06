Docker container with two Stream-Juggler REST interfaces (written on Scala and NodeJS),
available on one common port (proxied by nginx basing on rests uri path prefixes).

PREREQUISITES
Stream Juggler REST requires MongoDB.
In simple case (for local use) MongoDB can be started with command:
    docker run -d --name=mongo --net=host -v /tmp/Juggler/sj-mongo-storage:/data/db mongo
where -v dir:dir is for preventing the db data to lost when restarting container

Create dbs (if needed):
    docker exec -it mongo mongo
    > use stream_juggler

USAGE
Quick start with docker-compose:
    cd Juggler/docker
    docker-compose up
(@see how to install docker-compose: https://docs.docker.com/compose/install/ )

To build REST docker image:
    cd Juggler/docker && \
    docker build -t sj/rest --file=Dockerfile_rest ..

To run container:
    docker run -d --name=rest -e MONGO_HOST=<MONGO_IP> -p 8080:8080 -v /tmp/sj-rest-log:/var/log/rest sj/rest

Parameters (to be passed with -e on docker run)
    MONGO_HOST (*required) - The ip or hostname of Mongo DB
    MONGO_PORT (default: 27017) - The port of Mongo DB to connect to
    REST_PORT (default: 8080) - The port on which the REST will be available

Logs
All logs are available in /var/log/rest inside of container.
As an example, the log dir is volumed to /tmp/sj-rest-log on the host in the `docker run` command above.
Log files available:
    supervisor.log (combinded stdout and stderr)
    nginx.log
    nginx_error.log
    nginx_access.log
    rest.log
    rest_error.log
    rest_nodejs.log
    rest_nodejs_error.log