FROM ubuntu:trusty

MAINTAINER BITWORKS mendelbaum_nm@bw-sw.com

ENV NGINX_VERSION 1.8.0-1~jessie

RUN set -x && apt-get update

RUN apt-get install -y curl bzip2 git make build-essential

# Install nginx, nodejs
RUN curl -s http://nginx.org/keys/nginx_signing.key | apt-key add - && \
    echo "deb http://nginx.org/packages/debian/ jessie nginx" >> /etc/apt/sources.list && \
    curl -sL https://deb.nodesource.com/setup_6.x | bash - && \
    apt-get update && \
    apt-get install -y ca-certificates nginx=${NGINX_VERSION} nodejs && \
    rm -rf /var/lib/apt/lists/*

COPY ../ /opt/bin/juggler/core/sj-frontend

WORKDIR /opt/bin/juggler/core/sj-frontend

RUN npm install

CMD [ "npm", "start" ]


