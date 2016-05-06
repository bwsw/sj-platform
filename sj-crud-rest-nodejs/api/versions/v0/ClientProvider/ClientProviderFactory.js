// @todo It seems like Factory but it does not; maybe should be refactored

// @todo It depends to PROVIDER_TYPES from provider.js. It must be one entry point
var PROVIDERS = {
    cassandra: 'CassandraClient',
    aerospike: 'AerospikeClient',
    zookeeper: 'ZookeeperClient',
    ES: 'ElasticClient',
    redis: 'RedisClient',
    kafka: 'KafkaClient'
};

module.exports = function (provider) {
    var concreteClient = require('./' + PROVIDERS[provider.type]);
    return new concreteClient(provider);
};