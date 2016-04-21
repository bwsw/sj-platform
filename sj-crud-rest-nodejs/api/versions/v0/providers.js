var bodyParser = require('body-parser');
var mongoTools  = require("./models/mongo.js");
var mongoOp = new mongoTools('provider');
var ClientProviderFactory = require("./ClientProvider/ClientProviderFactory");
var PROVIDER_STRUCTURE = ['name', 'description', 'hosts', 'login', 'password', 'type'];
var PROVIDER_TYPES = ['cassandra', 'aerospike', 'zookeeper', 'kafka', 'ES', 'redis'];


module.exports = function (app) {
    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({'extended': true}));
    app.route('/providers')
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
            for (var key in body) {
                if (PROVIDER_STRUCTURE.indexOf(key) == -1) {
                    res.status(400).send('Provider structure must be ' + PROVIDER_STRUCTURE + '. Wrong key: ' + key);
                    throw new Error('Error');
                }
            }
            for (var key in PROVIDER_STRUCTURE) {
                if (body[PROVIDER_STRUCTURE[key]] == '' || body[PROVIDER_STRUCTURE[key]] == null) {
                    res.status(400).send('Provider structure must be ' + PROVIDER_STRUCTURE);
                    throw new Error('Error');
                }
                if (PROVIDER_STRUCTURE[key] == 'type') {
                    var is_allowed = false;
                    for (var type_key in PROVIDER_TYPES) {
                        if (body[PROVIDER_STRUCTURE[key]] == PROVIDER_TYPES[type_key]) {
                            is_allowed = true;
                        }
                    }
                    if (is_allowed == false) {
                        res.status(400).send('Type must be one of the following values : ' + PROVIDER_TYPES);
                        throw new Error('Error');
                    }
                }
                if (PROVIDER_STRUCTURE[key] == 'hosts') {
                    body['hosts'].forEach(function(item, i) {
                        if (body['type'] == 'kafka') {
                            var found = item.match(/^(\S+)\s(\S+)\s+(\S+)/);
                            console.log(found);
                            if (found != null) {
                                res.status(400).send('Hosts elements for "kafka" provider must be valid "IPv4:port ZkPrefix" strings.');
                                throw new Error('Error');
                            }
                        }
                        if (!validateIpAndPort(item, body['type'])) {
                            res.status(400).send('Hosts elements must be valid "IPv4:port" strings.');
                            throw new Error('Error');
                        }
                    });
                }
            }
            var provider = new mongoOp({
                name: body['name'],
                description: body['description'],
                hosts: body['hosts'],
                login: body['login'],
                password: body['password'],
                type: body['type']});
            provider.save(function (err, entity) {
                if (err) {
                    console.log(err);
                } else {
                    console.log('Created provider with id ' + entity.id);
                    res.status(201).send('Created provider id is: ' + entity.id);
                }});

        });
    app.route('/providers/:id')
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
                            message = {"error" : true,"message" : 'Error deleting provider'};
                        } else {
                            message = {"error" : false, "message" : 'Provider with id ' + req.params.id + ' deleted successfully'};
                        }
                        res.json(message);
                    });
                }
            });
        });
    app.route('/providers/:id/connection')
        .get(function (req, res) {
            mongoOp.findById(req.params.id, function (err, provider) {
                if (err) return res.json({ error: true, message: 'Error fetching data' });
                // Check availability
                var client = new ClientProviderFactory(provider);
                client.isAvailable().then(function (data) {
                    res.json(data);
                }).catch(function (data) {
                    // @todo Decide mapping to http status codes res.status(...)
                    res.json(data);
                });
            });
        });
};

/** Validates hosts strings
 * @param input Host string from POST request
 * @param type Provider type
 * @returns {boolean} Does input string is a valid string
 */
function validateIpAndPort(input, type) {
    if (type == 'kafka') {
        var found = input.match(/^(\S+)\s(\S+)/);
        input = found[1];
    }
    var parts = input.split(":");
    var ip = parts[0].split(".");
    var port = parts[1];
    return validateNum(port, 1, 65535) && ip.length == 4 && ip.every(function (segment) {
            return validateNum(segment, 0, 255);
        });
}

function validateNum(input, min, max) {
    var num = +input;
    return num >= min && num <= max && input === num.toString();
}