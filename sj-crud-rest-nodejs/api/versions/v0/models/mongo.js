var mongoose    =   require("mongoose");
var config = require('./../../../../config.json');
mongoose.connect('mongodb://' + config.api.db.ip + '/' + config.api.db.name);
// create instance of Schema
var mongoSchema =   mongoose.Schema;
// create schemas
var providerSchema  = mongoSchema({
    'name' : String,
    'description': String,
    'hosts': [String],
    'login': String,
    'password': String,
    'type': String
});
var serviceSchema  = mongoSchema({
    'name' : String,
    'description': String,
    'provider': String,
    'type': String,
    'keyspace':String,
    'index': Number,
    'namespace': String,
    'metadata_provider': String,
    'data_provider': String,
    'lock_provider': String
});
var streamSchema  = mongoSchema({
    'name' : String,
    'description': String,
    'partitions': String,
    'service': String,
    'tags':String,
    'generator':[String],
    'type': String
});
var db = mongoose.connection;
db.on('error', console.error.bind(console, 'connection error:'));
db.once('open', function() {
    console.log('Successfully connected to MongoDB')
});
module.exports = function (type) {
    if (type == 'provider') {
        return mongoose.model('providers', providerSchema);
    }
    else if (type == 'service') {
        return mongoose.model('services', serviceSchema);
    }
    else if (type == 'stream') {
        return mongoose.model('streams', streamSchema);
    }
};