#!/bin/bash

echo MONGO_HOST: ${MONGO_HOST?Please provide MONGO_HOST}

if [ -n "$PORT" ]; then echo "PORT env var is voided" && exit; fi

if [ -n "$HOST" ]; then echo "HOST env var is voided" && exit; fi

if [ -z "$MONGO_PORT" ]
then
   export MONGO_PORT=27017
   echo "Using default MONGO_PORT 27017"
else
   echo "Using MONGO_PORT $MONGO_PORT"
fi

if [ -z "$REST_PORT" ]
then
   export REST_PORT=8080
   echo "REST is starting on default $REST_PORT port..."
else
   echo "REST is starting on $REST_PORT port..."
fi

# Cfg nginx proxy
sed -i 's/listen 8080/listen '"$REST_PORT"'/g' /etc/nginx/conf.d/rest-nginx.conf

# Cfg for scala REST
export PORT=3002
export HOST=0.0.0.0

# Cfg for nodejs REST
PORT_NODEJS=3001
sed -i 's/"port": 3001/"port": '"$PORT_NODEJS"'/g' sj-crud-rest-nodejs/config.json
sed -i 's/"ip": "localhost"/"ip": "'"$MONGO_HOST"'"/g' sj-crud-rest-nodejs/config.json
sed -i 's/"port": 27017/"port": "'"$MONGO_PORT"'"/g' sj-crud-rest-nodejs/config.json

echo "Running. See logs in /var/log/rest"
supervisord -n > /var/log/rest/supervisor.log 2>&1