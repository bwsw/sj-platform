var bodyParser = require('body-parser');
var mongoTools  = require("./models/mongo.js");
var mongoOp = new mongoTools('service');
var mongoProviderOp = new mongoTools('provider');
var SERVICE_BASE_STRUCTURE = ['name', 'description', 'type'];
var SERVICE_TYPES = ['CassDB', 'ESInd', 'KfkQ', 'TstrQ', 'ZKCoord', 'RdsCoord', 'ArspkDB'];
var SERVICES_CUSTOM_FIELDS = {
    CassDB : {
        fields : ['keyspace', 'provider'],
        provider_types : ['cassandra']
    },
    ESInd : {
        fields : ['index', 'provider'],
        provider_types : ['ES']
    },
    KfkQ : {
        fields : ['provider'],
        provider_types : ['kafka']
    },
    TstrQ : {
        fields : ['namespace', 'metadata_provider', 'data_provider', 'lock_provider'],
        metadata_provider_types : ['cassandra'],
        data_provider_types : ['cassandra', 'aerospike'],
        lock_provider_types : ['zookeeper', 'redis']
    },
    ZKCoord : {
        fields : ['namespace', 'provider'],
        provider_types : ['zookeeper']
    },
    RdsCoord : {
        fields : ['namespace', 'provider'],
        provider_types : ['redis']
    },
    ArspkDB : {
        fields : ['namespace', 'provider'],
        provider_types : ['aerospike']
    }
};

module.exports = function (app) {
    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({'extended': true}));
    app.route('/services')
        .get(function (req, res) {
            var response = {};
            mongoOp.find({}, function (err, data) {
                if (err) {
                    response = {
                        error: true,
                        message: 'Error fetching data'
                    }
                } else {
                    response = {
                        error: false,
                        message: data
                    }
                }
                res.json(response);
            })
        })
        .post(function (req, res) {
            console.log(req.body);
            var body = req.body;
            //Check for correct service type
            if (SERVICE_TYPES.indexOf(body['type']) == -1) {
                res.status(400).send('Service type must be one of the following values: ' + SERVICE_TYPES);
                throw new Error('Error');
            }
            //Concat fields for current service
            if (body['type'] == '' || body['type'] == null) {
                res.status(400).send('Service structure must be ' + SERVICE_BASE_STRUCTURE);
            }
            else {
                var CURRENT_SERVICE_FIELDS = SERVICE_BASE_STRUCTURE.concat(SERVICES_CUSTOM_FIELDS[body['type']]['fields']);
            }
            //Check POST for unnecessary fields
            for (var key in body) {
                if (CURRENT_SERVICE_FIELDS.indexOf(key) == -1) {
                    res.status(400).send('Service structure of type ' + body['type'] + ' must be: ' + CURRENT_SERVICE_FIELDS + '. Wrong key: ' + key);
                    throw new Error('Error');
                }
            }
            //Check existence of fields
            CURRENT_SERVICE_FIELDS.forEach(function(field, i) {
                if (body[field] == '' || body[field] == null) {
                    res.status(400).send('Service structure for this type must be ' + CURRENT_SERVICE_FIELDS);
                    throw new Error('Error');
                }
                //Check provider type
                if (/(provider)/.test(field)) {
                    var type_is_allowed = false;
                    var current_provider_types = SERVICES_CUSTOM_FIELDS[body['type']][field +'_types'];
                    console.log(current_provider_types);
                    mongoProviderOp.find({_id:body[field]}, function (err, data) {
                        if (err || data.length == 0) {
                            res.json({error: true, message: 'Specified provider not found'});
                            throw new Error('Error');
                        } else {
                            var provider_type = data[0]['type'];
                            current_provider_types.forEach(function(item, i) {
                                if (provider_type == item) {
                                    type_is_allowed = true;
                                }
                            });
                            if (type_is_allowed == false) {
                                res.status(400).send('Type of ' + field + ' must be one of the following values : ' +
                                    current_provider_types + '. Specified provider type is ' + provider_type);
                                throw new Error('Error');
                            }
                        }
                    });
                }
            });
            var service = new mongoOp({
                name: body['name'],
                description: body['description'],
                type:body['type'],
                provider: body['provider'],
                keyspace: body['keyspace'],
                index: body['index'],
                namespace: body['namespace'],
                metadata_provider: body['metadata_provider'],
                data_provider: body['data_provider'],
                lock_provider: body['lock_provider']
            });
           service.save(function (err, entity) {
                if (err) {
                    console.log(err);
                } else {
                    console.log('Created service with id ' + entity.id);
                    res.status(201).send('Created service id is: ' + entity.id);
                }});

        });
    app.route('/services/:id')
        .get(function (req, res) {
            var response = {};
            mongoOp.findById(req.params.id, function (err, data) {
                if (err) {
                    response = {error: true, message: 'Error fetching data'};
                } else {
                    response = {error: false, message: data}
                }
                res.json(response);
            });
        })
        .delete(function (req, res) {
            var message = {};
            mongoOp.findById(req.params.id, function(err, data){
                if(err) {
                    res.status(404).send();
                } else {
                    mongoOp.remove({_id : req.params.id},function(err){
                        if(err) {
                            message = {"error" : true,"message" : 'Error deleting service'};
                        } else {
                            message = {"error" : false, "message" : 'Service with id ' + req.params.id + ' deleted successfully'};
                        }
                        res.json(message);
                    });
                }
            });
        });
};