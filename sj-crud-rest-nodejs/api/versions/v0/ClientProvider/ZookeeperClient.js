var zookeeper = require('node-zookeeper-client');
var Promise = require('promise');

var Client = function(provider) {
    this.provider = provider;
};

Client.prototype.isAvailable = function () {

    console.log(this.provider.hosts);
    var client = zookeeper.createClient(this.provider.hosts[0], {sessionTimeout: 3000});
    client.connect();
    return new Promise(function (resolve, reject) {
        var connected = false;

        client.on('connected', function () {
            client.close();
            resolve({ error: false, message: 'Zookeeper is available' });
        });
        client.on('connectedReadOnly', function () {
            client.close();
            resolve({ error: false, message: 'Zookeeper is available readonly' });
        });
        client.on("disconnected", function (){
            reject({message: "disconnected from zk"});
        });
        client.on('error', function (){
            reject({message: "Error from zk"});
        });
        setTimeout(function () {
            if (client.getState() == 'DISCONNECTED[0]')  reject({ error: true, message: 'Zookeeper is not available', detail: 'Timeout'});
            console.log("client state: %s", client.getState());
        }, 1000);

    }.bind(this));
};

module.exports = Client;