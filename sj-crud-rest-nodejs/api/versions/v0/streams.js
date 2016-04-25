var bodyParser = require('body-parser');
var Promise = require('promise');
var mongoTools  = require("./models/mongo.js");
var mongoOp = new mongoTools('stream');
var mongoServiceOp = new mongoTools('service');
var STREAM_STRUCTURE = ['name', 'description', 'partitions', 'service', 'type', 'tags', 'generator'];
var STREAM_TYPES = ['Tstream', 'kafka'];
var GENERATOR_VALUES = ['global', 'local', 'private'];
var STREAMS_CUSTOM_TYPES = {
    Tstream : {
        service_types : ['TstrQ']
    },
    kafka : {
        service_types : ['KfkQ']
    }
};

module.exports = function (app) {
    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({'extended': true}));
    app.route('/streams')
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
            var body = req.body;
            var validator_res = new Promise(function(resolve, reject) {
                var data = validate_stream(body);
                resolve(data);
            });
            validator_res.then(function(validator) {
                if (validator.error == true) {
                        res.status(validator.status).send({error:validator.error, message:validator.message});
                    }
                    else {
                        save_stream(body, res);
                    }
            }, function(validator) {
                if (validator.error == true) {
                    res.status(validator.status).send({error:validator.error, message:validator.message});
                }
                else {
                    save_stream(body, res);
                }
            });
        });
    app.route('/streams:id')
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
                            message = {"error" : true,"message" : 'Error deleting stream'};
                        } else {
                            message = {"error" : false, "message" : 'Stream with id ' + req.params.id + ' deleted successfully'};
                        }
                        res.json(message);
                    });
                }
            });
        });
};

function save_stream(body, res) {
    var stream = new mongoOp({
        name: body['name'],
        description: body['description'],
        partitions: body['partitions'],
        service: body['service'],
        type: body['type'],
        tags: body['tags'],
        generator: body['generator']
    });
    stream.save(function (err, entity) {
        if (err) {
            console.log(err);
            res.status(200).send(err);
        } else {
            console.log('Created stream with id ' + entity.id);
            res.status(201).send('Created stream id is: ' + entity.id);
        }
    });
}


function validate_stream(body, res) {
    //Check for unnecessary fields
    for (var key in body) {
        if (STREAM_STRUCTURE.indexOf(key) == -1) {
            return({error:true, status: 400, message: 'Stream structure must be ' + STREAM_STRUCTURE + '. Wrong key: ' + key});
        }
    }

    //Check for correct stream type
    if (STREAM_TYPES.indexOf(body['type']) == -1) {
        return({error:true, status: 400, message: 'Service type must be one of the following values: ' + STREAM_TYPES});
    }

    //Check existence of fields
    for (var index in STREAM_STRUCTURE) {
        if (STREAM_STRUCTURE.hasOwnProperty(index)) {
            var field = STREAM_STRUCTURE[index];
            if (body[field] == '' || body[field] == null) {
                return ({error: true, status: 400, message: 'Stream structure must be ' + STREAM_STRUCTURE + '. Missing field: ' + field});
            }
        }
            //Check generator for containing required values
            if (field == 'generator') {
                var found = 0;
                for (var i = 0; i< body['generator'].length; i++) {
                    for (var value_key in GENERATOR_VALUES) {
                        console.log('Check ' + body['generator'][i] + ' / ' + GENERATOR_VALUES[value_key]);
                        if (body['generator'][i] == GENERATOR_VALUES[value_key]) {
                            found = found + 1;
                        }
                    }
                }
                console.log('Found = ' + found);
                if (found != 1) {
                    return({
                        error: true,
                        status: 400,
                        message: 'Generator should contain at least one of the following words only once: ' + GENERATOR_VALUES
                    });
                }
            }
    }

    //Check service type
    return new Promise(function(resolve, reject) {
        var type_is_allowed = false;
        var current_service_types = STREAMS_CUSTOM_TYPES[body['type']]['service_types'];
        mongoServiceOp.find({_id:body['service']}, function (err, data) {
               if (err || data.length == 0) {
                    reject({error:true, status: 400, message: 'Specified service not found'});
               } else {
                    var service_type = data[0]['type'];
                        current_service_types.forEach(function(item, i) {
                            if (service_type == item) {
                                type_is_allowed = true;
                            }
                        });
                        if (type_is_allowed == false) {
                            reject({error:true, status: 400, message:'Type of ' + field + ' must be one of the following values : ' +
                            current_service_types + '. Specified service type is ' + service_type});
                        }
                         else {
                            resolve ({error:false});
                        }
                    }
                });

    });
}