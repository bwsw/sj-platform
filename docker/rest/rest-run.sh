#!/bin/bash

echo MONGO_HOSTS: ${MONGO_HOSTS?Please provide MONGO_HOSTS}

if [ -n "$REST_PORT" ]; then echo "REST_PORT env var is voided" && exit; fi

if [ -n "$REST_PORT" ]; then echo "REST_PORT env var is voided" && exit; fi

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
export CRUD_REST_PORT=3001
export CRUD_REST_HOST=0.0.0.0
export UI_PORT=5555
export UI_HOST=0.0.0.0

mkdir -p /var/log/rest
touch /var/log/rest/supervisor.log
echo "Running. See logs in /var/log/rest"
exec supervisord -n > /var/log/rest/supervisor.log 2>&1
