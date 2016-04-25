var redis = require('redis'); // https://www.npmjs.com/package/redis
var Promise = require('promise');
var _ = require('lodash');

// Docker for testing: https://hub.docker.com/_/aerospike/
// Default port :6379

var Client = function(provider) {
    this.provider = provider;
};

Client.prototype.isAvailable = function () {
    return new Promise(function (resolve, reject) {
        // @todo Only first host form provider.hosts
        console.log(this.provider.hosts);
        console.log(this.provider.hosts[1]);
        var splitted = this.provider.hosts[0].split(':');
        var config = { host: splitted[0], port: parseInt(splitted[1]) };
        var client = redis.createClient(config);

        client.on("error", function (err) {
            client.quit();
            reject({ error: true, message: 'Redis is not available', detail: err });
        });

        client.on("ready", function (err) {
            // @todo: Maybe we should resolve in callback of client.quit? We should wait when connection ended indeed
            client.quit();
            resolve({ error: false, message: 'Redis is available' });
        });
    }.bind(this));
};

module.exports = Client;