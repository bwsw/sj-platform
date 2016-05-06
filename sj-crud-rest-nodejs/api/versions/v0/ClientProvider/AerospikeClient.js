var aerospike = require('aerospike'); // https://github.com/aerospike/aerospike-client-nodejs/
var Promise = require('promise');
var _ = require('lodash');

// Docker for testing: https://hub.docker.com/_/aerospike/
// Default port :3000

var Client = function(provider) {
    this.provider = provider;
};

Client.prototype.isAvailable = function () {
    return new Promise(function (resolve, reject) {
        //Iterate over all hosts
        //for (var i=0; i<this.provider.hosts.length; i++){
        //
        //}
        var config = { hosts: _.map(this.provider.hosts, function (host) {
            var splitted = host.split(':');
            return { addr: splitted[0], port: parseInt(splitted[1]) }
        })};
        var client = aerospike.client({'hosts':'127.0.0.1:3003'});
        //console.log(aerospike.status);
        client.connect(function (err) {
            console.log(err.code);
            if (err.code != aerospike.status.AEROSPIKE_OK) reject({ error: true, message: 'Aerospike is not available', detail: err });
            resolve({ error: false, message: 'Aerospike is available' });
        });
    }.bind(this));
};

module.exports = Client;