var elastic = require('elasticsearch'); // https://www.npmjs.com/package/elasticsearch
var Promise = require('promise');

// Docker for testing: https://hub.docker.com/_/elasticsearch/
// Default port :9200

var Client = function(provider) {
    this.provider = provider;
};

Client.prototype.isAvailable = function () {
    return new Promise(function (resolve, reject) {
        var message = '';
        var length = this.provider.hosts.length;
        this.provider.hosts.forEach(function(item, i, length, message) {
            var client = new elastic.Client({
                hosts: item,
                log: 'trace'
            });
            client.ping({
                requestTimeout: Infinity,
                hello: 'elasticsearch!'
            }, function (err) {
                if (err) message = message.concat(item + "unavailable")
            });
            if (i == length-1) {
                console.log('TEST + ' + i);
            }
            //client.close();
        });
        resolve({ message: message });
    }.bind(this));
};

module.exports = Client;