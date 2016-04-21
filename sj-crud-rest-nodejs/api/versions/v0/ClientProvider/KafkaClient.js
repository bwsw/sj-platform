var kafka = require('kafka-node'); // https://github.com/SOHU-Co/kafka-node/
// @todo Decide which lib is more useful: https://www.npmjs.com/package/node-kafka - required node 0.10.x
var Promise = require('promise');


var net = require('net');

//https://cwiki.apache.org/confluence/display/KAFKA/A+Guide+To+The+Kafka+Protocol#AGuideToTheKafkaProtocol-Network
//Kafka uses a binary protocol over TCP. No handshake is required on connection or disconnection. So we just try to connect via TCP
//and if we can connect it means that kafka is available;

// Docker for testing: https://hub.docker.com/r/spotify/kafka/
// Default port :2181

var Client = function(provider) {
    this.provider = provider;
};

Client.prototype.isAvailable = function () {


    console.log('Starting connection');
    return new Promise(function (resolve, reject) {


        var client = new net.Socket();
        var splitted = this.provider.hosts[0].split(':');
        client.connect(splitted[1], splitted[0], function() {
            console.log('Connected by TCP');
            resolve({ error: false, message: 'Kafka is available.'});
            client.destroy();
        });
        client.on('error', function(err){
            reject({ error: true, message: 'Kafka is not available', detail: err })
        });


        // @todo Only first host form provider.hosts
        //client is a zookeeper client
        //var client = new kafka.Client(this.provider.hosts[0]);
        //console.log('Client created');
        //client.connect();
        //var Producer = kafka.Producer;
        //var producer = new Producer(client);
        //console.log('Producer created');


        // All of this horrible code is fighting with next issue:
        // client-lib doesn't return anything when host is not available
        // I try to handle some specific events about error but there is only solution for this: settimeout and rejection

        // @todo Mutex and semaphores in JS
        // as I read that function calls resolves in atomic way, so
        // if we in some function we can think that some using variable are locked out of another functions
        // 'connected' using like semaphore
        //var connected = false;

        //var timeOutCounter = setTimeout(function () {
        //    //semaphore
        //    if (!connected) {
        //        connected = true;
        //        reject({ error: true, message: 'Zookeeper is not available', detail: 'TimeOut' });
        //    }
        //}.bind(this), this.timeOut);

        //producer.on('ready', function () {
        //    client.close();
        //    producer.close();
        //    resolve({ error: false, message: 'Kafka is available.'});
        //});
        //
        //// If host not available this freezing, I don't know how to bind endless reconnection state
        //
        //// producer.on('disconnected', function (err) {
        ////     reject({ error: true, message: 'Zookeeper is not available', detail: err })
        //// });
        ////
        // producer.on('error', function (err) {
        //     client.close();
        //     producer.close();
        //     console.log('Producer error');
        //     reject({ error: true, message: 'Kafka is not available', detail: err })
        // });
        // producer.on('uncaughtException', function (err) {
        //     reject({ error: true, message: 'Zookeeper is not available', detail: err })
        // });
        // producer.on('close', function (err) {
        //     reject({ error: true, message: 'Zookeeper is not available', detail: err })
        // });
        // producer.on('brokersChanged', function (err) {
        //     reject({ error: true, message: 'Zookeeper is not available', detail: err })
        // });
        // producer.on('zkReconnect', function (err) {
        //     reject({ error: true, message: 'Zookeeper is not available', detail: err })
        // });
    }.bind(this));
};

module.exports = Client;