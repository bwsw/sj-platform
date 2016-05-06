Description
=

Frontend system for Stream-Juggler project. It includes UI (Angular 2) and REST-API (NODE.JS, Express).

Developing
=

Prerequisites
-
    npm install -g gulp
    npm install -g pm2
    npm install promise
    npm install lodash
    npm install cassandra-driver
    npm install elasticsearch

    npm install redis


    npm install

Development
-
* edit config.json
* run MongoDB server


    gulp api::start

Todos
-
* UI
    * Angular 2 skeleton
* API
    * translations
    * unit-testing
    * JSDoc / API Doc
    * pure Express or something else for API?
        * https://habrahabr.ru/post/222259/
        * https://github.com/marmelab/awesome-rest#nodejs