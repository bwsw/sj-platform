var cassandra = require('cassandra-driver'); // https://github.com/datastax/nodejs-driver
var Promise = require('promise');

// Docker for testing: https://hub.docker.com/_/cassandra/
// Default port :9042

var Client = function(provider) {
    this.provider = provider;
};

Client.prototype.isAvailable = function () {
    return new Promise(function (resolve, reject) {


        var client = new cassandra.Client({
            contactPoints: this.provider.hosts
        });
        client.connect(function (err) {
            if (err) reject({ error: true, message: 'Cassandra is not available', detail: err });
            resolve({ error: false, message: 'Cassandra is available' });
        });
    }.bind(this));
};

module.exports = Client;